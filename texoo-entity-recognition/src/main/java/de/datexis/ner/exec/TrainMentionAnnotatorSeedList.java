package de.datexis.ner.exec;

import com.google.common.primitives.Ints;
import de.datexis.common.CommandLineParser;
import de.datexis.common.Resource;
import de.datexis.common.WordHelpers;
import de.datexis.encoder.impl.*;
import de.datexis.model.Annotation;
import de.datexis.model.Dataset;
import de.datexis.ner.MatchingAnnotator;
import de.datexis.ner.MentionAnnotator;
import de.datexis.reader.RawTextDatasetReader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * Main Controller for training of MentionAnnotator / NER models.
 * @author Sebastian Arnold <sarnold@beuth-hochschule.de>
 */
public class TrainMentionAnnotatorSeedList {

  protected final static Logger log = LoggerFactory.getLogger(TrainMentionAnnotatorSeedList.class);

  public static void main(String[] args) throws IOException {
    
    final ExecParams params = new ExecParams();
    final CommandLineParser parser = new CommandLineParser(params);
    
    try {
      parser.parse(args);
      new TrainMentionAnnotatorSeedList().runTraining(params);
      System.exit(0);
    } catch(ParseException e) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("texoo-train-ner-seed", "TeXoo: train MentionAnnotator with seed list", params.setUpCliOptions(), "", true);
      System.exit(1);
    }
   
  }
  
  protected static class ExecParams implements CommandLineParser.Options {

    protected String inputFiles;
    protected String seedList;
    protected String language;
    protected String outputPath = null;
    protected String embeddingsFile = null;
    protected boolean trainingUI = false;
    protected int epochs = 10;
    protected int examples = -1;

    @Override
    public void setParams(CommandLine parse) {
      inputFiles = parse.getOptionValue("i");
      seedList = parse.getOptionValue("s");
      outputPath = parse.getOptionValue("o");
      embeddingsFile = parse.getOptionValue("e");
      trainingUI = parse.hasOption("u");
      language = parse.getOptionValue("l", "en");
      epochs = Optional.ofNullable(Ints.tryParse(parse.getOptionValue("n", "10"))).orElse(10);
      examples = Optional.ofNullable(Ints.tryParse(parse.getOptionValue("m", "-1"))).orElse(10);
    }

    @Override
    public Options setUpCliOptions() {
      Options op = new Options();
      op.addRequiredOption("i", "input", true, "path or file name for raw input text");
      op.addRequiredOption("s", "seed", true, "path to seed list text file");
      op.addRequiredOption("o", "output", true, "path to create and store the model");
      op.addOption("m", "examples", true, "limit number of examples per epoch (default: all)");
      op.addOption("n", "epochs", true, "number of epochs (default: 10)");
      op.addOption("e", "embedding", true, "path to word embedding model (default: letter-trigrams)");
      op.addOption("l", "language", true, "language to use for sentence splitting and stopwords (EN or DE)");
      op.addOption("u", "ui", false, "enable training UI (http://127.0.0.1:9000)");
      return op;
    }
    
  }
    
  protected void runTraining(ExecParams params) throws IOException {

    // Configure parameters
    Resource inputPath = Resource.fromDirectory(params.inputFiles);
    Resource outputPath = Resource.fromDirectory(params.outputPath);
    Resource seedPath = Resource.fromDirectory(params.seedList);
    WordHelpers.Language lang = WordHelpers.getLanguage(params.language);

    // Read datasets
    Dataset train =new RawTextDatasetReader().read(inputPath);

    // Configure matcher
    MatchingAnnotator match = new MatchingAnnotator(MatchingAnnotator.MatchingStrategy.LOWERCASE);
    match.loadTermsToMatch(seedPath);
    match.annotate(train);
  
    // Initialize builder
    MentionAnnotator.Builder builder = new MentionAnnotator.Builder();
    
    // Configure input encoders (trigram, fasttext or word embeddings)
    Resource embeddingModel = Resource.fromFile(params.embeddingsFile);
    if(params.embeddingsFile == null) {
      TrigramEncoder trigram = new TrigramEncoder();
      trigram.trainModel(train.getDocuments(), 10);
      builder.withEncoders("tri", new PositionEncoder(), new SurfaceEncoder(), trigram);
    } else if(embeddingModel.getFileName().endsWith(".bin") || embeddingModel.getFileName().endsWith(".bin.gz")) {
      FastTextEncoder fasttext = new FastTextEncoder();
      fasttext.loadModel(embeddingModel);
      builder.withEncoders("ft", new PositionEncoder(), new SurfaceEncoder(), fasttext);
    } else {
      Word2VecEncoder word2vec = new Word2VecEncoder();
      word2vec.loadModel(embeddingModel);
      builder.withEncoders("emb", new PositionEncoder(), new SurfaceEncoder(), word2vec);
    }
  
    // Configure model parameters
    MentionAnnotator ner = builder
      .enableTrainingUI(params.trainingUI)
      .withTrainingParams(0.0001, 64, params.epochs)
      .withModelParams(512, 256)
      .withWorkspaceParams(1) // single worker
      .build();

    // Train model
    ner.trainModel(train, Annotation.Source.SILVER, lang, params.examples, false, true);

    // Save model
    System.out.println("saving model to path: " + outputPath);
    outputPath.toFile().mkdirs();
    ner.writeModel(outputPath);

  }

}