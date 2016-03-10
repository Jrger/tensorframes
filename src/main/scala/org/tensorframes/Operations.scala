package org.tensorframes

import org.apache.spark.sql.{Row, DataFrame}
import org.tensorflow.framework.GraphDef


class InputNotFoundException(inputs: Seq[String])
  extends Exception(s"The following inputs were not provided: ${inputs.mkString(", ")}")

class InvalidDimensionException()
  extends Exception(s"The tensor ??? was found to be of dimension ???, but it was expected to be of dimension" +
    s"???.")

class InvalidTypeException()
  extends Exception(s"The input is of type ???, but the TF graph expected an input of type ???")

/**
 * Basic operations that can be performed on DataFrames using TensorFlow.
 */
trait OperationsInterface {

  /**
   * Given a graph of computations, transforms each of the rows of the dataframe one after the other through the
   * graph of computations defined below.
   *
   * The data is fed into the flow graph by defining placeholders that have the name of a column in the dataframe.
   * All the inputs of the graph must be filled with some data from the dataframe, or a constant.
   *
   * Output schema:
   * The resulting dataframe has all the same columns as before, plus all the columns that correspond to each output.
   * It is an error if one of the graph outputs has the same name as a column of the DataFrame.
   *
   * The order of the columns is unspecified.
   *
   * @param dataframe
   * @param graph
   * @return
   */
  @throws[IllegalArgumentException]("If an input is not being filled by a column")
  @throws[IllegalArgumentException]("If one of the outputs has the same name as one of the original columns")
  @throws[InvalidDimensionException]("If some data in the dataframe has the wrong shape to be accepted by TF")
  @throws[InvalidDimensionException]("If some data in the dataframe has the wrong datatype to be accepted by TF")
  def mapRows(dataframe: DataFrame, graph: GraphDef, shapeHints: ShapeDescription): DataFrame
  // Input validation
  //  - the graph loads
  //  - the inputs are all filled
  //  - the inputs have the proper dimension (to the extent we can check that)
  //  - the output is defined in the graph
  //  - the output does not collide with existing columns

  /**
   * Transforms the data in a dataframe by applying transforms in blocks of data.
   *
   * This function performs a similar task as the function above, but it is optimized for compact, vectorized
   * representation of the data in Spark. Instead of working row by row, it takes batches of rows together and returns
   * batches of rows in its outputs as well.
   *
   * The graph must accepts inputs that are one dimension higher than the data in the dataframe. For example, if
   * a column contains integer, then the placeholder for this column must accept vectors, etc.
   *
   * If all the data columns are passed to TensorFlow, then the output may have a different number of rows (and
   * only in this situation). If some extra columns are not passed to TF, the output of TF must be of the same size
   * and in the same order as the input.
   *
   * @param dataframe
   * @param graph
   * @return
   */
  def mapBlocks(dataframe: DataFrame, graph: GraphDef, shapeHints: ShapeDescription): DataFrame

  /**
   * Uses TensorFlow to merge two rows together from the data, until there is one row left, and returns this row.
   *
   * The graph must obey the following naming conventions. For each column, called for example X, there mast be two
   * placeholders called X_1 and X_2, and an output called X.
   *
   * The type of the pairs of inputs and the output must be the same.
   *
   * The elements in the row will be returned in the same order as the variable names.
   *
   * @param dataFrame
   * @param graph
   * @return
   */
  def reduceRows(dataFrame: DataFrame, graph: GraphDef, shapeHints: ShapeDescription): Row

  /**
   * Vectorized version of the reducer.
   *
   * The naming conventions are a bit different: for each output X, there must be a placeholder with one additional
   * dimension (the first one) called X_input.
   *
   * @param dataFrame
   * @param graph
   * @return
   */
  def reduceBlocks(dataFrame: DataFrame, graph: GraphDef, shapeHints: ShapeDescription): Row

  /**
   * A string that contains detailed information about a dataframe, in particular relevant information
   * with respect to TensorFlow.
   * @param df
   * @return
   */
  def explain(df: DataFrame): String
}