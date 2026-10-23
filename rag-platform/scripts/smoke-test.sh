#!/usr/bin/env bash
# End-to-end check: upload a file, wait for indexing, ask a question through the OpenAI-compatible API.
# Usage: scripts/smoke-test.sh <file> <collection> "<question>"
set -euo pipefail
FILE=${1:?file}; COLLECTION=${2:?collection}; QUESTION=${3:?question}
set -a; source .env; set +a
API=http://localhost:8080

echo "1) Upload $FILE to '$COLLECTION'"
RESP=$(curl -fsS -H "X-API-Key: $RAG_ADMIN_API_KEY" -F "file=@$FILE" -F "collection=$COLLECTION" "$API/api/documents")
echo "   $RESP"
ID=$(echo "$RESP" | sed -E 's/.*"id":"([^"]+)".*/\1/')

echo "2) Wait for indexing"
for i in $(seq 1 180); do
  STATUS=$(curl -fsS -H "X-API-Key: $RAG_ADMIN_API_KEY" "$API/api/documents/$ID" | sed -E 's/.*"status":"([A-Z]+)".*/\1/')
  echo "   status=$STATUS"
  [ "$STATUS" = "INDEXED" ] && break
  [ "$STATUS" = "FAILED" ] && { curl -fsS -H "X-API-Key: $RAG_ADMIN_API_KEY" "$API/api/documents/$ID"; echo; exit 1; }
  sleep 5
done

echo "3) Ask (non-streaming)"
curl -fsS "$API/v1/chat/completions" \
  -H "Authorization: Bearer $RAG_OPENAI_API_KEY" -H "Content-Type: application/json" \
  -d "{\"model\":\"rag-$COLLECTION\",\"stream\":false,\"messages\":[{\"role\":\"user\",\"content\":\"$QUESTION\"}]}"
echo
