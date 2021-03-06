package hex.pca;

import hex.DataInfo;
import hex.Model;
import hex.ModelMetrics;
import hex.ModelMetricsPCA;
import water.DKV;
import water.Key;
import water.MRTask;
import water.fvec.Chunk;
import water.fvec.Frame;
import water.fvec.Vec;
import water.util.TwoDimTable;

public class PCAModel extends Model<PCAModel,PCAModel.PCAParameters,PCAModel.PCAOutput> {

  public static class PCAParameters extends Model.Parameters {
    public int _k = 1;                // Number of principal components
    public double _gamma = 0;         // Regularization
    public int _max_iterations = 1000;     // Max iterations
    public long _seed = System.nanoTime(); // RNG seed
    public DataInfo.TransformType _transform = DataInfo.TransformType.NONE; // Data transformation (demean to compare with PCA)
    public PCA.Initialization _init = PCA.Initialization.PlusPlus;
    public Key<Frame> _user_points;
    public Key<Frame> _loading_key;
    boolean _keep_loading = false;
  }

  public static class PCAOutput extends Model.Output {
    // Iterations executed
    public int _iterations;

    // Average change in objective function this iteration
    public double _avg_change_obj;

    // Final loading matrix (X)
    // public Frame _loadings;

    // Mapping from training data to lower dimensional k-space (Y)
    public double[][] _archetypes;

    // PCA output on XY
    // Principal components (eigenvectors) from SVD of XY
    public double[/*feature*/][/*k*/] _eigenvectors_raw;
    public TwoDimTable _eigenvectors;

    // Standard deviation of each principal component
    public double[] _std_deviation;

    // Importance of principal components
    // Standard deviation, proportion of variance explained, and cumulative proportion of variance explained
    public TwoDimTable _pc_importance;

    // If standardized, mean of each numeric data column
    public double[] _normSub;

    // If standardized, one over standard deviation of each numeric data column
    public double[] _normMul;

    public PCAOutput(PCA b) { super(b); }

    /** Override because base class implements ncols-1 for features with the
     *  last column as a response variable; for PCA all the columns are
     *  features. */
    @Override public int nfeatures() { return _names.length; }

    @Override public ModelCategory getModelCategory() {
      return ModelCategory.DimReduction;
    }
  }

  public PCAModel(Key selfKey, PCAParameters parms, PCAOutput output) { super(selfKey,parms,output); }

  @Override
  public ModelMetrics.MetricBuilder makeMetricBuilder(String[] domain) {
    return new ModelMetricsPCA.PCAModelMetrics(_parms._k);
  }

  @Override
  protected Frame scoreImpl(Frame orig, Frame adaptedFr, String destination_key) {
    Frame adaptFrm = new Frame(adaptedFr);
    for(int i = 0; i < _parms._k; i++)
      adaptFrm.add("PC"+String.valueOf(i+1),adaptFrm.anyVec().makeZero());

    new MRTask() {
      @Override public void map( Chunk chks[] ) {
        double tmp [] = new double[_output._names.length];
        double preds[] = new double[_parms._k];
        for( int row = 0; row < chks[0]._len; row++) {
          double p[] = score0(chks, row, tmp, preds);
          for( int c=0; c<preds.length; c++ )
            chks[_output._names.length+c].set(row, p[c]);
        }
      }
    }.doAll(adaptFrm);

    // Return the projection into principal component space
    int x = _output._names.length, y = adaptFrm.numCols();
    Frame f = adaptFrm.extractFrame(x, y); // this will call vec_impl() and we cannot call the delete() below just yet

    f = new Frame((null == destination_key ? Key.make() : Key.make(destination_key)), f.names(), f.vecs());
    DKV.put(f);
    makeMetricBuilder(null).makeModelMetrics(this, orig, Double.NaN);
    return f;
  }

  @Override
  protected double[] score0(double data[/*ncols*/], double preds[/*nclasses+1*/]) {
    assert data.length == _output._eigenvectors.getRowDim();
    for(int i = 0; i < _parms._k; i++) {
      preds[i] = 0;
      for (int j = 0; j < data.length; j++)
        preds[i] += (data[j] - _output._normSub[j]) * _output._normMul[j] * (double)_output._eigenvectors.get(j,i);
    }
    return preds;
  }

  @Override
  public Frame score(Frame fr, String destination_key) {
    Frame adaptFr = new Frame(fr);
    adaptTestForTrain(adaptFr, true);   // Adapt
    Frame output = scoreImpl(fr, adaptFr, destination_key); // Score

    Vec[] vecs = adaptFr.vecs();
    for (int i = 0; i < vecs.length; i++)
      if (fr.find(vecs[i]) != -1) // Exists in the original frame?
        vecs[i] = null;            // Do not delete it
    adaptFr.delete();
    return output;
  }
}
