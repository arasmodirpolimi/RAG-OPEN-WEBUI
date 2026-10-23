# RAG Platform — Option 1: Open WebUI + Java hybrid-search backend (local test edition)

Upload Word, PDF and image files, index them for **BM25 + vector (hybrid) search**, and chat with them
in **Open WebUI**. All document intelligence runs in two Java 21 / Spring Boot services built with
**Clean Architecture (Domain / Agent / Application / Impl)** and **SOLID**.

---

## 1. Architecture

```
 Browser ──► Open WebUI :3000 ──(OpenAI API /v1)──► rag-api :8080 ──► OpenSearch (BM25 + k-NN)
                                                       │  │  └──────► Ollama (bge-m3 embeddings, chat LLM)
 Admin / scripts ──(REST /api/documents)──────────────►│  └─────────► TEI reranker
                                                       ▼
                                   RabbitMQ (rag.ingest, rag.ingest.dlq)
                                                       ▼
                         rag-ingestion ──► Docling Serve (parse + OCR) ──► chunk ──► Ollama embed ──► OpenSearch
                          shared: PostgreSQL (document status) · files volume (originals)
```

| Service | Role |
|---|---|
| `open-webui` | Chat UI. Sees each collection as a model `rag-<collection>`, plus a `chat` model for direct LLM conversation. |
| `rag-api` | OpenAI-compatible chat endpoint, hybrid retrieval, reranking, grounded answers with citations; document upload API; DB migration; index creation. |
| `rag-ingestion` | Queue consumer: parse (Docling) → chunk → embed → index. |
| `docling` | Converts PDF/DOCX/images to Markdown, with OCR for scans and photos. |
| `ollama` (+`ollama-init`) | `bge-m3` embeddings (1024-d, multilingual) and the local chat model. |
| `tei-rerank` | Cross-encoder reranker `Alibaba-NLP/gte-multilingual-reranker-base`. |
| `opensearch` | One index, two representations per chunk: Italian/English analysed text (BM25) and an HNSW vector. |
| `postgres` | Document registry and status (`PENDING → PROCESSING → INDEXED / FAILED`). |
| `rabbitmq` | Ingestion jobs with retries and a dead-letter queue. |

---

## 2. Clean Architecture layers

Interpretation used here of **application / Agent / Impl**, plus the Domain core:

| Layer (Maven module) | Contains | May depend on | Framework code |
|---|---|---|---|
| **Domain** `rag-domain` | Entities and value objects (`SourceDocument`, `Chunk`, `Citation`…), state rules, domain exceptions | nothing | none |
| **Agent** `rag-agent` | The AI/knowledge logic: structure-aware chunking, hybrid retrieval + RRF + reranking, query rewriting, grounded prompt + answer generation. Declares the ports it needs (`EmbeddingPort`, `KeywordSearchPort`, `VectorSearchPort`, `RerankPort`, `CompletionPort`, `StreamingCompletionPort`) | Domain | none |
| **Application** `rag-application` | Use cases (input ports) and their services; output ports for storage, queue, parser, repository, index | Agent, Domain | none |
| **Impl** `rag-impl` | Adapters implementing every port: OpenSearch, Ollama, Claude, TEI, Docling, PostgreSQL, filesystem, RabbitMQ; Spring wiring per technology | all inner layers | Spring |
| **Services** `rag-api-service`, `rag-ingestion-service` | Composition roots, web/messaging entry points, `application.yml` | everything | Spring Boot |

The dependency rule is enforced by the Maven module graph: an inner module **cannot compile** against
an outer one. Domain, Agent and Application have zero framework dependencies and are unit-tested in
milliseconds with fakes.

### SOLID, concretely

| Principle | Where |
|---|---|
| **S**ingle responsibility | `RrfFusion` only fuses rankings; `PromptBuilder` only builds prompts; `QueryRewritingAgent` only rewrites; `OpenAiSseWriter` only writes SSE frames; each adapter speaks to one system. |
| **O**pen/closed | New LLM, vector store, parser or file store = new adapter class + one bean; no use case changes (e.g. `AnthropicChatAdapter` was added beside `OllamaChatAdapter`). |
| **L**iskov substitution | Any `EmbeddingPort`/`KeywordSearchPort`/… implementation is interchangeable; tests substitute lambdas for real adapters and the services behave identically. |
| **I**nterface segregation | `CompletionPort` (rewriting) is separate from `StreamingCompletionPort` (answering); `KeywordSearchPort` and `VectorSearchPort` are separate; `ChunkIndexPort` (write) is separate from the search ports (read). |
| **D**ependency inversion | Agents and use cases depend on ports they own; adapters depend inward. Concrete classes are bound only in `ApiCompositionRoot` / `IngestionCompositionRoot`. |

### Package map

```
rag-domain       com.ragplatform.domain.{model,exception}
rag-agent        com.ragplatform.agent.{port,chunking,retrieval,conversation,answer}
rag-application  com.ragplatform.application.{port.in,port.out,service}
rag-impl         com.ragplatform.impl.{config,http,opensearch,ollama,anthropic,reranker,docling,
                                       persistence,storage,messaging,wiring}
rag-api-service        com.ragplatform.api.{config,web.openai,web.documents,web.security}
rag-ingestion-service  com.ragplatform.ingestion.{config,messaging}
```

---

## 3. End-to-end flows

**Ingestion**

1. `POST /api/documents` (multipart `file`, `collection`; header `X-API-Key`).
2. `UploadDocumentService`: validates the collection, SHA-256 de-duplication per collection, stores the original, saves `PENDING`, enqueues the id.
3. `ProcessDocumentService` (in `rag-ingestion`): `PROCESSING` → Docling `/v1/convert/file` (Markdown, OCR `it,en`, page-break placeholder for page numbers) → `MarkdownStructureChunker` → batch embeddings → OpenSearch replace (delete-by-query + bulk) → `INDEXED`.
4. On failure: `FAILED` with the reason; RabbitMQ retries 3× (10 s, 20 s), then dead-letters to `rag.ingest.dlq`. `POST /api/documents/{id}/reindex` re-queues.

**Chunking rules (`MarkdownStructureChunker`)**: each heading starts a new chunk and extends the
section path (`Art. 7 > Penali`); paragraphs are packed to ~400 tokens with ~50 tokens of overlap on
size-based splits; tables are separate chunks (split by rows, repeating the header, if large); lines
repeated on most pages (letterheads, fixed footers) are removed. The section path is prepended to the
text for embeddings and reranking, and indexed (boosted) for BM25.

**Question answering (RAG models)**

1. Open WebUI → `POST /v1/chat/completions` (`model = rag-<collection>`, full history, `stream: true`).
2. `QueryRewritingAgent`: follow-ups become standalone questions (first questions skip the LLM).
3. `HybridRetrievalAgent`: BM25 (`multi_match` on `text`, `text.en`, `section_path^2`) and k-NN (filter inside the `knn` clause) in parallel, 50 each → RRF (k = 60) → top 30 → TEI rerank → top 6 (optional score threshold).
4. No sources → fixed "not found" reply, **the LLM is not called**. Otherwise `AnswerGenerationAgent` streams an answer that may only use the numbered sources and must cite them as `[n]`.
5. `rag-api` streams OpenAI `chat.completion.chunk` events and appends a **Fonti / Sources** list (file, page(s), section).

**General chat (no documents)**

Selecting the `chat` model in Open WebUI sends the conversation directly to the LLM (Ollama or
Claude, depending on `LLM_PROVIDER`) with no retrieval, chunking, or citation. This is useful for
general questions that do not require document grounding. The model name is configurable via
`rag.answer.general-chat-model` (env `RAG_ANSWER_GENERAL_CHAT_MODEL`, default `chat`).

**Unified model (chat + RAG)**

The `assistant` model combines both behaviours: it searches **all** configured collections for
relevant sources. When matches are found, the answer is grounded with citations (like the `rag-*`
models); when no documents match, the conversation is forwarded directly to the LLM (like `chat`).
This gives a single model that works for general conversation and document Q&A. Configurable via
`rag.answer.unified-model` (env `RAG_ANSWER_UNIFIED_MODEL`, default `assistant`).

---

## 4. Run it locally

**Prerequisites**: Docker with Compose v2 and BuildKit; about **16 GB RAM** for the default models
(use `CHAT_MODEL=qwen2.5:3b` on smaller machines); ~25 GB free disk for images and models; internet
access on first start (images, Ollama models, reranker weights from Hugging Face).

```bash
cp .env.example .env              # then set RAG_OPENAI_API_KEY and RAG_ADMIN_API_KEY
docker compose up -d --build
docker compose logs -f ollama-init   # wait until both models are pulled, then Ctrl+C
docker compose ps                    # rag-api should be "healthy"
```

Useful local endpoints:

| URL | What |
|---|---|
| http://localhost:3000 | Open WebUI (the first account you create becomes admin) |
| http://localhost:8080/actuator/health | rag-api health |
| http://localhost:5001/ui | Docling playground: inspect how a file is parsed |
| http://localhost:15672 | RabbitMQ UI (`rag` / `rag`): queues `rag.ingest`, `rag.ingest.dlq` |
| http://localhost:9200/rag-chunks/_search?q=penale | Raw look at indexed chunks |

### Test sequence

1. **Unit tests** (no Docker needed): `mvn test`. The Domain, Agent and Application tests run with fakes.
2. **Smoke test** of the whole pipeline:
   ```bash
   scripts/smoke-test.sh ./mio-contratto.pdf contratti "Qual è la penale per ritardo nella consegna?"
   ```
   It uploads, polls the status until `INDEXED`, then asks through the OpenAI-compatible API.
3. **In Open WebUI**: choose the model `rag-contratti`, ask a question, check the citations, then ask
   a follow-up ("e per i servizi?") to exercise query rewriting.
4. **Test the hard inputs**: a scanned PDF, a phone photo of a page (JPG), a Word file with tables,
   and a question whose answer is *not* in the documents (expect the "not found" reply).
5. **Failure path**: stop Docling (`docker compose stop docling`), upload a file, confirm it ends
   `FAILED` with a reason and lands in `rag.ingest.dlq`; restart Docling and call `/reindex`.

Manual API calls:

```bash
source .env
curl -H "X-API-Key: $RAG_ADMIN_API_KEY" -F file=@doc.pdf -F collection=contratti http://localhost:8080/api/documents
curl -H "X-API-Key: $RAG_ADMIN_API_KEY" "http://localhost:8080/api/documents?collection=contratti"
curl -H "Authorization: Bearer $RAG_OPENAI_API_KEY" http://localhost:8080/v1/models
```

### Open WebUI notes

- The Compose file sets the OpenAI connection, disables the Ollama connection, and disables title,
  follow-up and tag generation, so Open WebUI's background tasks never hit the RAG endpoint.
- Open WebUI stores many settings in its database after the first start (`ConfigVar` persistence):
  later changes to these environment variables may not apply until you change them in **Admin
  Settings** or reset the `openwebui` volume.
- Do not attach files in the Open WebUI chat for these models: attachments go to Open WebUI's own
  RAG, not to this pipeline. Upload through `/api/documents`.
- With `ENABLE_FORWARD_USER_INFO_HEADERS=true`, `rag-api` receives `X-OpenWebUI-User-Email` and logs it.

### Using Claude instead of the local model

Set `LLM_PROVIDER=anthropic` and `ANTHROPIC_API_KEY` in `.env`, then `docker compose up -d rag-api`.
Embeddings and reranking stay local; only the retrieved excerpts and the question are sent to the API.

---

## 5. Configuration reference (`rag-impl/src/main/resources/rag-common.yml`)

Every key can be overridden by environment variable (Spring relaxed binding: `rag.ollama.chat-model`
→ `RAG_OLLAMA_CHAT_MODEL`). Main tuning knobs: `rag.chunking.max-tokens` / `overlap-tokens`,
`rag.retrieval.*`, `rag.reranker.enabled` / `min-score`, `rag.collections`.

`rag.opensearch.dimension` must equal the embedding model's output size; the embedding adapter refuses
vectors of any other size instead of corrupting the index. Changing the model therefore means a new
index: set a new `index-name`, point the alias to it, and reindex the documents.

---

## 6. Verification record — external contracts and their sources

Every integration is coded against the provider's own documentation, checked on 23 Oct 2026:

| Contract used in code | Source checked |
|---|---|
| Docling Serve `POST /v1/convert/file` multipart (`files`, `to_formats`, `do_ocr`, `ocr_lang` as BCP-47, `image_export_mode`, `md_page_break_placeholder`, `do_pdf_heading_hierarchy`, `abort_on_error`); response `document.md_content`, `status`; image `quay.io/docling-project/docling-serve-cpu`; port 5001; `DOCLING_SERVE_MAX_SYNC_WAIT` (default 120 s) | docling-serve `docs/usage.md`, `docs/configuration.md`, `README.md` |
| Ollama `POST /api/embed` (`model`, `input` list → `embeddings`), `POST /api/chat` (NDJSON stream, `message.content`, `done`, `options.temperature`) | ollama `docs/api.md` |
| `bge-m3` on Ollama, 1024 dimensions | Ollama library model listings |
| TEI `POST /rerank` (`query`, `texts`, `truncate`) → `[{index, score}]`; image `cpu-1.9` / `cpu-arm64-1.9`; container port 80; `gte-multilingual-reranker-base` supported | text-embeddings-inference `README.md`, `docs/openapi.json` |
| OpenSearch `knn_vector` + `hnsw` / `lucene` / `cosinesimil`; `knn` query with `filter` inside the clause; image `opensearchproject/opensearch:3.5.0` | OpenSearch k-NN filtering docs; Docker Hub tags |
| Claude Messages API: `POST /v1/messages`, headers `x-api-key`, `anthropic-version: 2023-06-01`; SSE `content_block_delta` / `text_delta`, `message_stop`, `error` | Claude streaming documentation |
| Open WebUI env vars `OPENAI_API_BASE_URL`, `OPENAI_API_KEY`, `ENABLE_OLLAMA_API`, `ENABLE_TITLE_GENERATION`, `ENABLE_FOLLOW_UP_GENERATION`, `ENABLE_TAGS_GENERATION`, `ENABLE_FORWARD_USER_INFO_HEADERS`; header `X-OpenWebUI-User-Email` | Open WebUI `env-configuration` reference |
| Spring Boot 3.5.5 | spring.io release announcements |

### What was executed vs. what you must run

- **Executed here**: the Domain, Agent and Application modules were compiled with `javac --release 21
  -Xlint:all -Werror`, and their **29 JUnit 5 tests passed**. The Compose file and YAML files were
  parsed; the POMs are well-formed.
- **Not executed here** (Maven Central was unreachable from the build sandbox): compilation of
  `rag-impl` and the two Spring Boot services, and the Docker stack. Run `mvn verify` and the smoke
  test locally; please report any compile error and it can be fixed precisely.

---

## 7. Known limitations (by design for a local test)

- **Word files have no pages** in the parser's model, so they are cited by section (shown as `p. 1`).
- **Re-indexing a document** deletes then re-adds its chunks; during that window it is not searchable.
- **Upload is not transactional** across file store, database and queue; a document stuck in
  `PENDING` is recovered with `/reindex`.
- **The reranker threshold** is disabled (`min-score: 0.0`) until calibrated on your own questions.
- **Security is local-grade**: no TLS, OpenSearch security disabled, shared API keys.
- **Files are stored on a Docker volume**: add an S3-compatible `FileStoragePort` adapter for servers.

## 8. Next steps toward production

1. **Golden set**: build 50 real questions per collection with expected file and page. Add a JUnit +
   Testcontainers suite measuring recall@10 and MRR for BM25-only, vector-only, hybrid, and hybrid+rerank.
2. **Tuning**: adjust chunk size, overlap, candidate counts, and the rerank threshold from those numbers.
3. **Security**: TLS via a reverse proxy, OpenSearch security, secrets manager, and per-collection
   authorisation from the forwarded user headers.
4. **Observability**: OpenTelemetry tracing per retrieval stage (Micrometer Tracing).
5. **Reliability**: Docling's async API for very large files, pinned image versions, an S3 storage adapter.
