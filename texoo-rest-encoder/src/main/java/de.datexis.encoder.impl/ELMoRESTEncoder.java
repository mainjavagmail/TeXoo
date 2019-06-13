package de.datexis.encoder.impl;

import de.datexis.model.Document;
import de.datexis.model.Sentence;
import de.datexis.model.Span;
import de.datexis.model.Token;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;

/**
 * Created by philipp on 02.10.18.
 */
public class ELMoRESTEncoder extends AbstractRESTEncoder {
  private static final Logger log = LoggerFactory.getLogger(ELMoRESTEncoder.class);

  public static ELMoRESTEncoder create(ELMoLayerOutput elMoLayerOutput, String domain, int port) {
    return new ELMoRESTEncoder(new ELMoRESTAdapter(elMoLayerOutput, domain, port));
  }

  public static ELMoRESTEncoder create(ELMoLayerOutput elMoLayerOutput, String domain, int port, String vectorIdentifier) {
    return new ELMoRESTEncoder(new ELMoRESTAdapter(elMoLayerOutput, domain, port), vectorIdentifier);
  }

  public ELMoRESTEncoder(RESTAdapter restAdapter) {
    super(restAdapter);
  }

  public ELMoRESTEncoder(RESTAdapter restAdapter, String vectorIdentifier) {
    super(restAdapter, vectorIdentifier);
  }

  @Override
  public INDArray encode(String word) {
    throw new UnsupportedOperationException();
  }

  @Override
  public INDArray encode(Span span) {
    throw new UnsupportedOperationException();
  }

  @Override
  public INDArray encode(Iterable<? extends Span> spans) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void encodeEach(Sentence input, Class<? extends Span> elementClass) {
    if(elementClass==Token.class){
      try {
        encodeEach1D(input.getTokens());
      } catch (IOException e) {
        log.error("IO Error while encoding sentence: {}", input, e);
        throw new UncheckedIOException(e);
      }
    }else{
      throw new UnsupportedOperationException("ELMo can not encode anything else then Tokens");
    }
  }

  @Override
  public void encodeEach(Document input, Class<? extends Span> elementClass) {
    if(elementClass==Token.class){
      try {
        encodeEach2D(getTokensOfSentencesOfDocument(input));
      } catch (IOException e) {
        log.error("IO Error while encoding document: {}", input.getTitle(), e);
        throw new UncheckedIOException(e);
      }
    }else{
      throw new UnsupportedOperationException("ELMo can not encode anything else then Tokens");
    }
  }

  @Override
  public void encodeEach(Collection<Document> docs, Class<? extends Span> elementClass) {
    for (Document document : docs) {
      encodeEach(document, elementClass);
    }
  }

  @Override
  public INDArray encodeMatrix(List<Document> input, int maxTimeSteps, Class<? extends Span> timeStepClass) {
    throw new UnsupportedOperationException();
  }
}
