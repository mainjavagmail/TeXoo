package de.datexis.encoder.skipthought;

import de.datexis.encoder.AbstractRESTAdapter;
import de.datexis.encoder.serialization.DeserializationProvider;
import de.datexis.encoder.serialization.JacksonProvider;
import de.datexis.encoder.serialization.SerializationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class SkipthoughtRESTAdapter extends AbstractRESTAdapter {
  private static final Logger log = LoggerFactory.getLogger(SkipthoughtRESTAdapter.class);

  public static final int DEFAULT_READ_TIMEOUT = 300000;
  public static final int DEFAULT_CONNECT_TIMEOUT = 10000;
  public static final long DEFAULT_EMBEDDING_VECTOR_SIZE = 4800;

  public static final String URL_FORMAT = "http://%s:%d/v2/%s";

  public static final String SENTENCE_ENDPOINT = "embed/sentences";
  public static final String SENTENCES_ENDPOINT = "embed/sentences";

  private String domain;
  private int port;

  private JacksonProvider serdeProvider;

  public SkipthoughtRESTAdapter(String domain, int port, long embeddingVectorSize, int connectTimeout, int readTimeout) {
    super(embeddingVectorSize, connectTimeout, readTimeout);
    this.domain = domain;
    this.port = port;

    serdeProvider = new JacksonProvider();
  }

  public SkipthoughtRESTAdapter(String domain, int port) {
    this(domain, port, DEFAULT_EMBEDDING_VECTOR_SIZE, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
  }

  @Override
  public double[] encodeImpl(String sentence) throws IOException{
    return request(sentence, double[].class, getUrl(SENTENCE_ENDPOINT));
  }

  @Override
  public double[][] encodeImpl(String[] sentencesOfDocument) throws IOException{
    return request(sentencesOfDocument, double[][].class, getUrl(SENTENCES_ENDPOINT));
  }

  @Override
  public double[][][] encodeImpl(String[][] data) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public SerializationProvider getSerializationProvider() {
    return serdeProvider;
  }

  @Override
  public DeserializationProvider getDeserializationProvider() {
    return serdeProvider;
  }

  public URL getUrl(String path) throws MalformedURLException {
      return new URL(String.format(URL_FORMAT, domain, port, path));
  }
}
