package de.datexis.encoder.elmo;

import com.google.common.collect.Lists;
import de.datexis.encoder.DummyRESTAdapter;
import de.datexis.encoder.RESTAdapter;
import de.datexis.model.Document;
import de.datexis.model.Sentence;
import de.datexis.model.Token;
import de.datexis.preprocess.DocumentFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ELMoRESTEncoderTest {
  public static final int EMBEDDING_VECTOR_SIZE = 100;

  private final String DUMMY_TEXT = "This is a sentence.";

  private List<Document> dummyDocuments;
  private Document dummyDocument;
  private Sentence dummySentence;
  private Token dummyToken;

  private RESTAdapter restAdapter;
  private ELMoRESTEncoder elMoRESTEncoder;

  @Before
  public void setup() throws IOException {
    dummyDocument = DocumentFactory.fromText(DUMMY_TEXT);
    dummySentence = dummyDocument.getSentence(0);
    dummyToken = dummySentence.getToken(0);
    dummyDocuments = Lists.newArrayList(dummyDocument);

    restAdapter = spy(new DummyRESTAdapter(EMBEDDING_VECTOR_SIZE));
    elMoRESTEncoder = spy(new ELMoRESTEncoder(restAdapter));
  }

  public void encodeImplTest() throws IOException {
    elMoRESTEncoder.encode(DUMMY_TEXT);

    verify(elMoRESTEncoder, times(1)).encodeValue(anyString());
  }

  public void encodeImplTokenTest() throws IOException {
    elMoRESTEncoder.encode(dummyToken);

    verify(elMoRESTEncoder, times(1)).encodeValue(anyString());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void encodeImplSentenceTest(){
    elMoRESTEncoder.encode(dummySentence);


  }

  @Test
  public void encodeEachImplTokenOfSentenceTest() throws IOException {
    elMoRESTEncoder.encodeEachImpl(dummySentence);

    verify(elMoRESTEncoder, times(1)).encodeEach1D(any());
  }

  @Test
  public void encodeEachImplTokenOfDocumentTest() throws IOException {
    elMoRESTEncoder.encodeEachImpl(dummyDocument);

    verify(elMoRESTEncoder, times(1)).getTokensOfSentencesOfDocument(eq(dummyDocument));

    verify(elMoRESTEncoder, times(1)).encodeEach2D(any());
  }

  @Test
  public void encodeEachImplTokenOfDocumentsTest() throws IOException {
    elMoRESTEncoder.encodeEachImpl(dummyDocuments);

    verify(elMoRESTEncoder, times(dummyDocuments.size()))
        .encodeEachImpl(Mockito.any(Document.class));
  }
}
