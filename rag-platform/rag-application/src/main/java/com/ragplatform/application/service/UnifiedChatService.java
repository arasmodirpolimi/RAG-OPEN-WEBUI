package com.ragplatform.application.service;

import com.ragplatform.agent.answer.AnswerGenerationAgent;
import com.ragplatform.agent.conversation.QueryRewritingAgent;
import com.ragplatform.agent.port.StreamingCompletionPort;
import com.ragplatform.agent.port.TokenSink;
import com.ragplatform.agent.retrieval.HybridRetrievalAgent;
import com.ragplatform.application.port.in.AnswerQuestionUseCase.AnswerResult;
import com.ragplatform.application.port.in.UnifiedChatUseCase;
import com.ragplatform.domain.model.ChatMessage;
import com.ragplatform.domain.model.Citation;
import com.ragplatform.domain.model.RetrievedChunk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Single model: searches every configured collection for relevant sources. When sources are found
 * the answer is grounded with citations; when nothing matches the conversation goes directly to
 * the LLM for a normal answer.
 */
public final class UnifiedChatService implements UnifiedChatUseCase {

    private final QueryRewritingAgent rewriter;
    private final HybridRetrievalAgent retriever;
    private final AnswerGenerationAgent generator;
    private final StreamingCompletionPort streaming;
    private final CollectionPolicy collections;
    private final int finalTopK;

    public UnifiedChatService(QueryRewritingAgent rewriter, HybridRetrievalAgent retriever,
                              AnswerGenerationAgent generator, StreamingCompletionPort streaming,
                              CollectionPolicy collections, int finalTopK) {
        this.rewriter = rewriter;
        this.retriever = retriever;
        this.generator = generator;
        this.streaming = streaming;
        this.collections = collections;
        this.finalTopK = finalTopK;
    }

    @Override
    public AnswerResult chat(List<ChatMessage> conversation, TokenSink sink) {
        String question = rewriter.standaloneQuestion(conversation);

        List<RetrievedChunk> allSources = new ArrayList<>();
        for (String collection : collections.all()) {
            allSources.addAll(retriever.retrieve(question, collection));
        }
        allSources.sort(Comparator.comparingDouble(RetrievedChunk::score).reversed());
        List<RetrievedChunk> sources = allSources.size() > finalTopK
                ? List.copyOf(allSources.subList(0, finalTopK))
                : List.copyOf(allSources);

        if (sources.isEmpty()) {
            streaming.stream(conversation, sink);
            return AnswerResult.notFound();
        }

        List<Citation> citations = generator.generate(question, conversation, sources, sink);
        return new AnswerResult(citations, true);
    }
}
