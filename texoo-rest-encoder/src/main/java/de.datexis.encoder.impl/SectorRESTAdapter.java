package de.datexis.encoder.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.datexis.encoder.impl.serde.DeserializationProvider;
import de.datexis.encoder.impl.serde.JacksonSerdeProvider;
import de.datexis.encoder.impl.serde.SerializationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class SectorRESTAdapter extends AbstractRESTAdapter {

  private static final Logger log = LoggerFactory.getLogger(SectorRESTAdapter.class);

  public static final int DEFAULT_READ_TIMEOUT = 300000;
  public static final int DEFAULT_CONNECT_TIMEOUT = 10000;
  public static final long DEFAULT_EMBEDDING_VECTOR_SIZE = 128;

  public static final String URL_FORMAT = "http://%s:%d/v2/%s";

  public static final String SENTENCE_ENDPOINT = "encode/sentence";
  public static final String DOCUMENT_ENDPOINT = "encode/document";

  /*public static final String HTTP_REQUEST_METHOD = "POST";
  public static final String HTTP_CONTENT_TYPE_NAME = "Content-Type";
  public static final String HTTP_CONTENT_TYPE_VALUE = "application/json; charset=UTF-8";*/

  private String domain;
  private int port;

  private JacksonSerdeProvider jacksonSerdeProvider;

  public SectorRESTAdapter(String domain, int port, long embeddingVectorSize, int connectTimeout, int readTimeout) {
    super(embeddingVectorSize, connectTimeout, readTimeout);
    this.domain = domain;
    this.port = port;

    jacksonSerdeProvider = new JacksonSerdeProvider();
  }

  public SectorRESTAdapter(String domain, int port) {
    this(domain, port, DEFAULT_EMBEDDING_VECTOR_SIZE, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
  }

  @Override
  public double[] encodeImpl(String data) throws IOException {
    return request(data, double[].class, getUrl(SENTENCE_ENDPOINT));
  }

  @Override
  public double[][] encodeImpl(String[] data) throws IOException {
    return request(data, double[][].class, getUrl(DOCUMENT_ENDPOINT));
  }

  @Override
  public double[][][] encodeImpl(String[][] data) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public SerializationProvider getSerializationProvider() {
    return jacksonSerdeProvider;
  }

  @Override
  public DeserializationProvider getDeserializationProvider() {
    return jacksonSerdeProvider;
  }

  /*public<I,O> O request(I data, String path, Class<O> classOfO) throws IOException{
    HttpURLConnection httpConnection = null;

    try {
      log.debug("building request");
      httpConnection = getConnection(path);
      log.debug("connect to: {}", httpConnection.getURL());
      httpConnection.connect();

      log.debug("writing to: {}", httpConnection.getURL());
      writeRequestBody(data, httpConnection);

      log.debug("reading from: {}", httpConnection.getURL());
      O responseData = readResponseData(httpConnection, classOfO);
      log.debug("response read from: {}", httpConnection.getURL());

      return responseData;

    } finally {
      if (httpConnection != null) {
        httpConnection.disconnect();
      }
    }

  }

  public <T> void writeRequestBody(T data, HttpURLConnection httpConnection) throws IOException {
    OutputStream outputStream = httpConnection.getOutputStream();
    objectMapper.writeValue(outputStream, data);
    outputStream.close();
  }

  public <T> T readResponseData(HttpURLConnection httpURLConnection, Class<T> classOfT)
      throws IOException {
    InputStream inputStream = httpURLConnection.getInputStream();
    T responseData = objectMapper.readValue(inputStream, classOfT);
    inputStream.close();
    return responseData;
  }

  public HttpURLConnection configureConnection(String path) throws IOException{
    HttpURLConnection httpConnection = getConnection(path);
    httpConnection.setRequestMethod(HTTP_REQUEST_METHOD);
    httpConnection.setRequestProperty(HTTP_CONTENT_TYPE_NAME,HTTP_CONTENT_TYPE_VALUE);
    httpConnection.setDoOutput(true);
    httpConnection.setDoInput(true);

    return httpConnection;
  }

  private HttpURLConnection getConnection(String path) throws IOException {
    URL url = getUrl(path);
    return (HttpURLConnection) url.openConnection();
  }*/

  private URL getUrl(String path) throws MalformedURLException {
    return new URL(String.format(URL_FORMAT, domain, port, path));
  }
}
