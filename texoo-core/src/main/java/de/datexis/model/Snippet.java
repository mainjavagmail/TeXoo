package de.datexis.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An extract of Sentence or Token References used for Training or Inference.
 * Please note, that offsets are not valid in this data structure and Annotations are not automatically copied.
 * New Annotations should be added to the original Documents. 
 * @author Sebastian Arnold <sarnold@beuth-hochschule.de>
 */
public class Snippet extends Document {

  protected final static Logger log = LoggerFactory.getLogger(Snippet.class);
  
  /** default constructor for JSON deserialization */
  protected Snippet() {};
  
  /**
   * Create a Snippet from existing Document. Snippet is extended to Sentence boundaries.
   */ 
  public Snippet(Document documentRef, int begin, int length) {
    this.sentences = documentRef.streamSentencesInRange(begin, begin + length, false).collect(Collectors.toList());
    this.setDocumentRef(documentRef);
    if(!sentences.isEmpty()) {
      this.setBegin(sentences.get(0).getBegin());
      this.setEnd(sentences.get(sentences.size() - 1).getEnd());
    } else {
      this.setBegin(begin);
      this.setEnd(begin + length);
    }
  }
  
  /**
   * Create a Snippet with existing Sentences.
   */
  @Deprecated
  public Snippet(Collection<Sentence> sentences, boolean randomizeOrder) {
    if(!sentences.isEmpty()) {
      List<Sentence> list = new ArrayList<>(sentences);
      if(randomizeOrder) Collections.shuffle(list, new Random(System.nanoTime()));
      this.sentences = list;
    }
  }
  
  /**
   * Appends a Sentence to the end of the Snippet. Offsets and Reference are not changed.
   * @param s The Sentence to add.
   */
  @Override
  public void addSentence(Sentence s) {
    addSentence(s, false);
	}
  
   /**
   * Appends a Sentence to the end of the Snippet. Offsets and Reference are not changed.
   * @param s The Sentence to add.
   * @param adjustOffsets ignored
   */
  @Override
  public void addSentence(Sentence s, boolean adjustOffsets) {
    sentences.add(s);
  }
  
  /**
   * Adds a single Annotation to the original Document. Original Document Reference is kept.
   * @param <A> Type of the Annotation
   */
  @Override
  public <A extends Annotation> void addAnnotation(A ann) {
    ann.getDocumentRef().addAnnotation(ann);
  }
 
  /**
   * Adds a List of Annotations to their original Documents. 
   * @param anns The Annotations to add. Original Document Reference is kept.
   */
  @Override
  public void addAnnotations(List<? extends Annotation> anns) {
    anns.stream().forEach(ann -> ann.getDocumentRef().addAnnotation(ann));
  }

  /**
   * @return all Annotations from referring document that are contained in this sample
   */
  @JsonIgnore
  @Override
  protected Stream<? extends Annotation> streamAnnotations() {
    if(getDocumentRef() == null) return Stream.empty();
    else return getDocumentRef().streamAnnotations()
        .filter(a -> a.getBegin() >= this.getBegin() && a.getEnd() <= this.getEnd());
  }

  @Override
  public long countAnnotations() {
    return streamAnnotations().count();
  }

}
