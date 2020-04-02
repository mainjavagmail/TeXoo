package de.datexis.ner.tagger;

import com.google.common.collect.Lists;
import de.datexis.encoder.Encoder;
import de.datexis.encoder.EncoderSet;
import de.datexis.encoder.EncodingHelpers;
import de.datexis.model.*;
import de.datexis.model.tag.BIO2Tag;
import de.datexis.model.tag.BIOESTag;
import de.datexis.model.tag.Tag;
import de.datexis.ner.MentionAnnotation;
import de.datexis.ner.eval.MentionAnnotatorEval;
import de.datexis.ner.eval.MentionTaggerEval;
import de.datexis.tagger.AbstractIterator;
import de.datexis.tagger.Tagger;
import org.apache.commons.lang3.tuple.Pair;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.conf.layers.recurrent.Bidirectional;
import org.deeplearning4j.nn.conf.layers.recurrent.Bidirectional.Mode;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.parallelism.ParallelWrapper;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.shade.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Assigns BIO2 or BIOES Labels to every Token in a Document.
 * @author Sebastian Arnold <sarnold@beuth-hochschule.de>
 */
public class MentionTagger extends Tagger {

  protected static final Logger log = LoggerFactory.getLogger(MentionTagger.class);
  
  protected int workers = 1;
  
  protected Class<? extends Tag> tagset = BIOESTag.class;
  protected String type = Tag.GENERIC;
  
  public MentionTagger() {
    this("BLSTM");
    setTagset(BIOESTag.class, Tag.GENERIC);
  }
  
  public MentionTagger(String id) {
    super(id);
    setTagset(BIOESTag.class, Tag.GENERIC);
  }
  
  public MentionTagger(AbstractIterator data, int ffwLayerSize, int lstmLayerSize, int iterations, double learningRate) {
    super(data.getInputSize(), data.getLabelSize());
		net = createBLSTM(inputVectorSize, ffwLayerSize, lstmLayerSize, outputVectorSize, iterations, learningRate);
	}
  
  public MentionTagger setModelParams(int ffwLayerSize, int lstmLayerSize, int iterations, double learningRate) {
    net = createBLSTM(inputVectorSize, ffwLayerSize, lstmLayerSize, outputVectorSize, iterations, learningRate);
    return this;
  }
  
  public Class<? extends Tag> getTagset() {
    return tagset;
  }
  
  public static ComputationGraph createBLSTM(long inputVectorSize, long ffwLayerSize, long lstmLayerSize, long outputVectorSize, int iterations, double learningRate) {

		log.info("initializing BLSTM network " + inputVectorSize + ":" + ffwLayerSize + ":" + ffwLayerSize + ":" + lstmLayerSize + ":" + outputVectorSize);
		ComputationGraphConfiguration.GraphBuilder gb = new NeuralNetConfiguration.Builder()
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				//.updater(new RmsProp(0.95))
        .updater(new Adam(learningRate, 0.9, 0.999, 1e-8))
        .l2(0.0001)
        .trainingWorkspaceMode(WorkspaceMode.ENABLED)
        .inferenceWorkspaceMode(WorkspaceMode.ENABLED)
				.graphBuilder()
        .addInputs("input");
      if(ffwLayerSize > 0) {
        gb.addLayer("FF1", new DenseLayer.Builder()
            .nIn(inputVectorSize).nOut(ffwLayerSize)
            .activation(Activation.RELU)
            .weightInit(WeightInit.RELU)
            .build(), /*new RnnToFeedForwardPreProcessor(),*/ "input")
        .addLayer("FF2", new DenseLayer.Builder()
            .nIn(ffwLayerSize).nOut(ffwLayerSize)
            .activation(Activation.RELU)
            .weightInit(WeightInit.RELU)
            .build(), "FF1")
        .addLayer("BLSTM", new Bidirectional(Mode.AVERAGE, new LSTM.Builder()
            .nIn(ffwLayerSize).nOut(lstmLayerSize)
						.activation(Activation.TANH)
            .weightInit(WeightInit.XAVIER)
            //.dropOut(0.5)
            .build()), "FF2");
      } else {
        gb.addLayer("BLSTM", new Bidirectional(Mode.AVERAGE, new LSTM.Builder()
            .nIn(inputVectorSize).nOut(lstmLayerSize)
						.activation(Activation.TANH)
            .weightInit(WeightInit.XAVIER)
            //.dropOut(0.5)
            .build()), "input");
      }
        gb.addLayer("output", new RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
            .nIn(lstmLayerSize).nOut(outputVectorSize)
            .activation(Activation.SOFTMAX)
            .weightInit(WeightInit.XAVIER)
            .build(), "BLSTM")
        .setOutputs("output")
        .setInputTypes(InputType.recurrent(inputVectorSize))
				.backpropType(BackpropType.Standard)
        .build();

    ComputationGraphConfiguration conf = gb.build();
		ComputationGraph lstm = new ComputationGraph(conf);
		lstm.init();
		return lstm;
    
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
  
  public MentionTagger setTagset(Class<? extends Tag> tagset) {
    this.tagset = tagset;
    try {
      this.outputVectorSize = tagset.newInstance().getVectorSize();
    } catch (Exception ex) {
      log.error("Could not set output vector size");
    }
    return this;
  }
  
  public MentionTagger setTagset(Class<? extends Tag> tagset, String types) {
    setTagset(tagset);
    this.type = types;
    return this;
  }
  
  public MentionTagger setTrainingParams(int batchSize, int numEpochs, boolean randomize) {
    this.batchSize = batchSize;
    this.numEpochs = numEpochs;
    this.randomize = randomize;
    return this;
  }
  
  public MentionTagger setWorkspaceParams(int workers) {
    this.workers = workers;
    return this;
  }
  
  @JsonIgnore
  @Deprecated
  public EncoderSet getEncoderSet() {
    return new EncoderSet(getEncoders().toArray(new Encoder[]{}));
  }
  
  public void trainModel(Dataset dataset) {
    trainModel(dataset, Annotation.Source.GOLD);
  }
  
  public void trainModel(Dataset dataset, Annotation.Source trainingAnnotations) {
    trainModel(dataset, trainingAnnotations, -1, randomize);
  }
  
  public void trainModel(Dataset dataset, Annotation.Source trainingAnnotations, int numExamples, boolean randomize) {
    trainModel(new MentionTaggerIterator(dataset.getDocuments(), dataset.getName(), getEncoderSet(), tagset, trainingAnnotations, numExamples, batchSize, randomize));
  }
  
  public void trainModel(Collection<Sentence> sentences, Annotation.Source trainingTags, boolean randomize) {
    trainModel(new MentionTaggerIterator(Lists.newArrayList(new Snippet(sentences, randomize)), "training", getEncoderSet(), tagset, trainingTags, -1, batchSize, randomize));
  }
  
  protected void trainModel(MentionTaggerIterator it) {
    int batches = it.numExamples() / it.batch();
    int n = 0;
    
    appendTrainLog("Training " + getName() + " with " + it.numExamples() + " examples in " + batches + " batches for " + numEpochs + " epochs.");
    
    // ParallelWrapper will take care of load balancing between GPUs.
    ParallelWrapper wrapper = null;
    if(workers > 1) {
      wrapper = new ParallelWrapper.Builder(net)
        .prefetchBuffer(workers * 4)  // DataSets prefetching options. Set this value with respect to number of actual devices
        .workers(workers)          // set number of workers equal or higher then number of available devices. x1-x2 are good values to start with
        //.averagingFrequency(1) // rare averaging improves performance, but might reduce model accuracy
        //.reportScoreAfterAveraging(false) // if set to TRUE, on every averaging model score will be reported
        .trainingMode(ParallelWrapper.TrainingMode.AVERAGING)
        .workspaceMode(WorkspaceMode.ENABLED)
        .build();
    }
    timer.start();
		for(int i = 1; i <= numEpochs; i++) {
      timer.setSplit("epoch");
      if(wrapper != null) {
        wrapper.fit(it);
      } else {
        if(net instanceof ComputationGraph) ((ComputationGraph) net).fit(it);
        else if(net instanceof MultiLayerNetwork) ((MultiLayerNetwork) net).fit(it);
      }
      n += it.numExamples();
			appendTrainLog("Completed epoch " + i + " of " + numEpochs + "\t" + n, timer.getLong("epoch"));
      it.reset();
		}
    timer.stop();
		appendTrainLog("Training complete", timer.getLong());
    setModelAvailable(true);
    
  }
  
  /**
   * Predicts labels for all Tokens in the Iterator and assigns Tags (BIO2 or BIOES).
   * requires: Encoder.class on Token.class (using parallelized DocumentIterator batches)
   * attaches: BIO2Tag.class to Token.class
   */
  @Override
  public void tag(Collection<Document> documents) {
    log.debug("Labeling Documents...");
    MentionTaggerIterator it = new MentionTaggerIterator(documents, "train", getEncoderSet(), tagset, -1, batchSize, false);
    it.reset();
		while(it.hasNext()) {
      // 1. Load a batch of sentences
      Pair<DataSet,ArrayList<Sentence>> examples = it.nextDataSet();
			INDArray input = examples.getKey().getFeatures();
      INDArray inputMask = examples.getKey().getFeaturesMaskArray();
      INDArray labelsMask = examples.getKey().getLabelsMaskArray();
      // 2. Predict labels
      INDArray predicted = null;
      synchronized(net) {
        if(net instanceof MultiLayerNetwork) {
          predicted = ((MultiLayerNetwork) net).output(input, false, inputMask, labelsMask);
        } else if(net instanceof ComputationGraph) {
          ((ComputationGraph) net).setLayerMaskArrays(new INDArray[]{inputMask}, new INDArray[]{labelsMask});
          predicted = ((ComputationGraph) net).outputSingle(input);
        }
      }
      // 3. Create BIOES tags from vectors + CRF and convert to BIO2 - RENAME
      createTags(examples.getValue(), predicted, it.getTagset(), Annotation.Source.PRED, type, false, true);
		}
    for(Document doc : it.getDocuments()) {
      doc.setTagAvailable(Annotation.Source.PRED, it.getTagset(), true);
      if(!tagset.equals(BIO2Tag.class)) doc.setTagAvailable(Annotation.Source.PRED, BIO2Tag.class, true);
    }
  }
  
  public void tagSentences(Collection<Sentence> sentences) {
    tag(Lists.newArrayList(new Snippet(sentences, false)));
  }
    
  /**
   * requires: GOLD BIO2Tag.class and BIOESTag.class for Token.class
   * attaches: PRED BIO2Tag.class and BIOESTag.class to Token.class
   * @param dataset
   * @param expected 
   */
  public void testModel(Dataset dataset, Annotation.Source expected) {
    
    // Tag Dataset using BIOES and finally produce BIO2 tags
    MentionTaggerIterator it = new MentionTaggerIterator(dataset.getDocuments(), dataset.getName(), getEncoderSet(), tagset, -1, batchSize, false);
    test(it);
    
    // Test tagging performance: BIOES or BIO2
    MentionTaggerEval eval = new MentionTaggerEval(getName(), tagset);
    eval.calculateMeasures(dataset);
    appendTestLog(eval.printExperimentStats());
    appendTestLog(eval.printDatasetStats());
    appendTestLog(eval.printTrainingCurve());
    appendTestLog(eval.printSequenceClassStats(false));
    
    // Test annotation performance: exact match using BIO2 tags
    MentionAnnotatorEval annE = new MentionAnnotatorEval(getName());
    for(Document doc : dataset.getDocuments()) {
      if(doc.countAnnotations(expected) == 0)
        MentionAnnotation.annotateFromTags(doc, expected, BIO2Tag.class, type);
      doc.clearAnnotations(Annotation.Source.PRED, MentionAnnotation.class);
      MentionAnnotation.annotateFromTags(doc, Annotation.Source.PRED, BIO2Tag.class, type);
    }
    annE.setTestDataset(dataset, 0, 0);
    annE.evaluateAnnotations();
    appendTestLog(annE.printAnnotationStats());
    
  }
  
  public Evaluation test(MentionTaggerIterator it) {
    timer.start();
    appendTrainLog("Evaluating " + getName() + " with " + it.numExamples() + " examples...");
    Evaluation eval = new Evaluation(it.getLabelSize());
    it.reset();
    while(it.hasNext()) {
      // 1. Load a batch of sentences
      Pair<DataSet,ArrayList<Sentence>> examples = it.nextDataSet();
			INDArray input = examples.getKey().getFeatures();
      INDArray labels = examples.getKey().getLabels();
      INDArray inputMask = examples.getKey().getFeaturesMaskArray();
      INDArray labelsMask = examples.getKey().getLabelsMaskArray();
      // 2. Predict labels
			INDArray predicted = null;
      if(net instanceof MultiLayerNetwork) {
        predicted = ((MultiLayerNetwork) net).output(input, false, inputMask, labelsMask);
      } else if(net instanceof ComputationGraph) {
        ((ComputationGraph) net).setLayerMaskArrays(new INDArray[]{inputMask}, new INDArray[]{labelsMask});
        predicted = ((ComputationGraph) net).outputSingle(input);
      }
      try {
        eval.evalTimeSeries(labels, predicted, labelsMask);
      } catch(IllegalStateException ex) {
        log.warn(ex.toString());
      }
      // 3. Create tags from labels
      createTags(examples.getValue(), predicted, it.getTagset(), Annotation.Source.PRED, type, true, true);
    }
    for(Document doc : it.getDocuments()) {
      doc.setTagAvailable(Annotation.Source.PRED, it.getTagset(), true);
      if(!tagset.equals(BIO2Tag.class)) doc.setTagAvailable(Annotation.Source.PRED, BIO2Tag.class, true);
    }
    timer.stop();
		appendTrainLog("Evaluation complete", timer.getLong());
    return eval;
  }
  
  public void enableTrainingUI() {
    StatsStorage stats = new InMemoryStatsStorage();
    net.addListeners(new StatsListener(stats, 1));
    UIServer.getInstance().attach(stats);
  }
  
  /**
   * Creates BIO2Tags from predictions.
   * requires: INDArray predictions for Token.class
   * attaches: BIO2Tag.class to Token.class
   * @param sents List of sentences to tag.
   * @param predicted Predictions from the sequence model.
   * @param tagset The tagset to use for tagging (e.g. BIOES). Output will be transformed to BIOES then.
   * @param source Which tags to use: GOLD, PREDICTED or USER.
   * @param keepVectors If TRUE, vectors are not deleted from the Tokens.
   * @param convertTags If TRUE, tags are corrected and converted to BIO2. Otherwise, we keep Tags of class tagset.
   */
  public static void createTags(Iterable<Sentence> sents, INDArray predicted, Class tagset, Annotation.Source source, String type, boolean keepVectors, boolean convertTags) {
    //System.out.println(predicted.toString());
    int batchNum = 0, t = 0;
    for(Sentence s : sents) {
      for(Token token : s.getTokens()) {
        INDArray vec = EncodingHelpers.getTimeStep(predicted, batchNum, t++);
        if(tagset.equals(BIO2Tag.class)) token.putTag(source, new BIO2Tag(vec, type, true));
        if(tagset.equals(BIOESTag.class)) token.putTag(source, new BIOESTag(vec, type, true));
      }
      t=0; batchNum++;
      if(tagset.equals(BIOESTag.class)) {
        BIOESTag.correctCRF(s, source);
        if(convertTags) BIOESTag.convertToBIO2(s, source);
      }
      // TODO: cleanup the vectors here now!
      if(!keepVectors) {
        
      }
    }
  }
  
}
