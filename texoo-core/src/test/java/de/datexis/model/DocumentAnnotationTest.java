package de.datexis.model;

import de.datexis.model.impl.PassageAnnotation;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;


public class DocumentAnnotationTest {

  private Annotation testAnnotation;
  private SubtypeAnnotation subtypeAnnotation;
  private Document testDocument;

  public DocumentAnnotationTest() {

  }

  @Before
  public void setUp() {
    prepareTestAnnotations();
    prepareTestDocument();
  }

  private void prepareTestDocument() {
    testDocument = new Document();
    testDocument.addAnnotation(testAnnotation);
    testDocument.addAnnotation(subtypeAnnotation);
  }

  private void prepareTestAnnotations() {
    testAnnotation = new PassageAnnotation(Annotation.Source.GOLD);
    testAnnotation.setBegin(0);
    testAnnotation.setEnd(10);
    subtypeAnnotation = new SubtypeAnnotation();
  }

  @Test
  public void streamAnnotationsIncludingSubtypesReturnsSubtypes() throws Exception {
    Stream<Annotation> annotationStream = testDocument.streamAnnotations(Annotation.class, true);
    List<Annotation> collect = annotationStream.collect(Collectors.toList());
    boolean containsTestAnnotation = collect.contains(testAnnotation);
    boolean containsNERAnnotation = collect.contains(subtypeAnnotation);

    assertTrue(containsTestAnnotation);
    assertTrue(containsNERAnnotation);
  }

  @Test
  public void streamAnnotationsIncludingSubtypesReturnsNotSupertype() throws Exception {
    Stream<SubtypeAnnotation> annotationStream = testDocument.streamAnnotations(SubtypeAnnotation.class, true);
    List<Annotation> collect = annotationStream.collect(Collectors.toList());
    boolean containsTestAnnotation = collect.contains(testAnnotation);
    boolean containsNERAnnotation = collect.contains(subtypeAnnotation);

    assertFalse(containsTestAnnotation);
    assertTrue(containsNERAnnotation);
  }

  @Test
  public void removeAnnotation() throws Exception {
    boolean removeAnnotationResult = testDocument.removeAnnotation(testAnnotation);
    boolean containsResult = testDocument.getAnnotations().contains(testAnnotation);

    assertTrue(removeAnnotationResult);
    assertFalse(containsResult);
  }
  
  /** empty Annotation class for testing purposes */
  protected class SubtypeAnnotation extends Annotation {
  }

}
