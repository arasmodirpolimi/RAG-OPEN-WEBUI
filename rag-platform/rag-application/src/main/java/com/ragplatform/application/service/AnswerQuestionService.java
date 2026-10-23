package com.ragplatform.application.service;

import com.ragplatform.agent.answer.AnswerGenerationAgent;
import com.ragplatform.agent.conversation.QueryRewritingAgent;
import com.ragplatform.agent.port.TokenSink;
import com.ragplatform.agent.retrieval.HybridRetrievalAgent;
import com.ragplatform.application.port.in.AnswerQuestionUseCase;
import com.ragplatform.domain.model.Citation;
import com.ragplatform.domain.model.RetrievedChunk;
import java.util.List;

/** Use case: rewrite -> retrieve -> (no sources: fixed reply | sources: grounded generation). */
public final class AnswerQuestionService implements AnswerQuestionUseCase {

    private final QueryRewritingAgent rewriter;
    private final HybridRetrievalAgent retriever;
    private final AnswerGenerationAgent generator;
    private final CollectionPolicy collections;
    private final String notFoundMessage;

    public AnswerQuestionService(QueryRewritingAgent rewriter, HybridRetrievalAgent retriever,
                                 AnswerGenerationAgent generator, CollectionPolicy collections,
                                 String notFoundMessage) {
        this.rewriter = rewriter;
        this.retriever = retriever;
        this.generator = generator;
        this.collections = collections;
        this.notFoundMessage = notFoundMessage;
    }

    @Override
    public AnswerResult answer(AskQuestionCommand command, TokenSink sink) {
        collections.requireKnown(command.collection());
        String question = rewriter.standaloneQuestion(command.conversation());
        List<RetrievedChunk> sources = retriever.retrieve(question, command.collection());
        if (sources.isEmpty()) {
            // No LLM call: the model is never given the chance to answer without evidence.
            sink.accept(notFoundMessage);
            return AnswerResult.notFound();
        }
        List<Citation> citations = generator.generate(question, command.conversation(), sources, sink);
        return new AnswerResult(citations, true);
    }
}
