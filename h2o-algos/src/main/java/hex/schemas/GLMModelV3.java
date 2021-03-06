package hex.schemas;

import hex.glm.GLMModel;
import water.MemoryManager;
import water.api.API;
import water.api.ModelOutputSchema;
import water.api.ModelSchema;

import water.util.ArrayUtils;
import water.api.TwoDimTableBase;
import water.util.TwoDimTable;

import java.util.Arrays;
import java.util.Comparator;
//import water.util.DocGen.HTML;

public class GLMModelV3 extends ModelSchema<GLMModel, GLMModelV3, GLMModel.GLMParameters, GLMV3.GLMParametersV3, GLMModel.GLMOutput, GLMModelV3.GLMModelOutputV3> {

  public static final class GLMModelOutputV3 extends ModelOutputSchema<GLMModel.GLMOutput, GLMModelOutputV3> {

    @API(help="Table of coefficients")
    TwoDimTableBase coefficients_table;

    @API(help="Coefficient magnitudes")
    TwoDimTableBase coefficients_magnitude;


    @Override
    public GLMModelOutputV3 fillFromImpl(GLMModel.GLMOutput impl) {
      super.fillFromImpl(impl);
      GLMModel.Submodel sm = impl.bestSubmodel();
      String [] cnames = impl.coefficientNames();
      String [] names = sm.idxs == null?impl.coefficientNames().clone():ArrayUtils.select(impl.coefficientNames(), sm.idxs);
      coefficients_table = new TwoDimTableBase();
      coefficients_magnitude = new TwoDimTableBase();
      final double [] magnitudes;

      if(sm.norm_beta != null){
        // coefficients_table = new TwoDimTable("Coefficients",impl._names,impl.isNormalized()? new String[]{"Coefficients, Normalized Coefficients"}: new String[]{"Coefficients"});
        String [] colTypes = new String[]{"double","double"};
        String [] colFormats = new String[]{"%5f", "%5f"};
        TwoDimTable tdt = new TwoDimTable("Coefficients","glm coefficients", names, new String[]{"Coefficients", "Norm Coefficients"}, colTypes, colFormats, "names");
        for(int i = 0; i < sm.beta.length; ++i) {
          tdt.set(i, 0, sm.beta[i]);
          tdt.set(i, 1, sm.norm_beta[i]);
        }
        coefficients_table.fillFromImpl(tdt);
        magnitudes = sm.norm_beta.clone();
        for(int i = 0; i < magnitudes.length; ++i)
          if(magnitudes[i] < 0) magnitudes[i] *= -1;
        tdt = new TwoDimTable("Coefficient Magnitudes","(standardized) coefficient magnitudes", names, new String[]{"Coefficients"},new String[]{"double"},new String[]{"%5f"},"names");
        for(int i = 0; i < sm.beta.length; ++i)
          tdt.set(i, 0, magnitudes[i]);
        coefficients_magnitude = new TwoDimTableBase();
        coefficients_magnitude.fillFromImpl(tdt);
      } else {
        // coefficients_table = new TwoDimTable("Coefficients",impl._names,impl.isNormalized()? new String[]{"Coefficients, Normalized Coefficients"}: new String[]{"Coefficients"});
        String [] colTypes = new String[]{"double"};
        String [] colFormats = new String[]{"%5f"};
        TwoDimTable tdt = new TwoDimTable("Coefficients","glm coefficients", names, new String[]{"Coefficients"}, colTypes, colFormats, "names");
        for(int i = 0; i < sm.beta.length; ++i)
          tdt.set(i,0,sm.beta[i]);
        coefficients_table.fillFromImpl(tdt);
        magnitudes = sm.beta.clone();
        for(int i = 0; i < magnitudes.length; ++i)
          if(magnitudes[i] < 0) magnitudes[i] *= -1;
      }
      Integer [] indices = new Integer[magnitudes.length-1];
      for(int i = 0; i < indices.length; ++i)
        indices[i] = i;

      Arrays.sort(indices, new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
          if(magnitudes[o1] < magnitudes[o2]) return +1;
          if(magnitudes[o1] > magnitudes[o2]) return -1;
          return 0;
        }
      });
      String [] names2 = new String[names.length];
      for(int i = 0; i < names2.length-1; ++i)
        names2[i] = names[indices[i]];
      names2[names2.length-1] = names[names.length-1];
      String [] colTypes = new String[]{"double"};
      String [] colFormats = new String[]{"%5f"};
      TwoDimTable tdt = new TwoDimTable("Coefficient Magnitudes","coefficient magnitudes", names2, new String[]{"Coefficients"},colTypes,colFormats,"names");
      for(int i = 0; i < sm.beta.length-1; ++i)
        tdt.set(i, 0, magnitudes[indices[i]]);
      tdt.set(sm.beta.length-1, 0, magnitudes[sm.beta.length-1]);
      coefficients_magnitude = new TwoDimTableBase();
      coefficients_magnitude.fillFromImpl(tdt);
      return this;
    }
  } // GLMModelOutputV2

  public GLMV3.GLMParametersV3 createParametersSchema() { return new GLMV3.GLMParametersV3(); }
  public GLMModelOutputV3 createOutputSchema() { return new GLMModelOutputV3(); }

  @Override public GLMModel createImpl() {
    GLMModel.GLMParameters parms = parameters.createImpl();
    return new GLMModel( model_id.key(), parms, new GLMModel.GLMOutput(), null, 0.0, 0.0, 0);
  }
}
