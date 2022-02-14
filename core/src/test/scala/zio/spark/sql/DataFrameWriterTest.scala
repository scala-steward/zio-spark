package zio.spark.sql

import org.apache.spark.sql.{Row, SaveMode}

import zio.Task
import zio.spark.helper.Fixture._
import zio.test._

import scala.reflect.io.Directory

import java.io.File

object DataFrameWriterTest {

  /** Deletes the folder generated by the test. */
  @SuppressWarnings(Array("scalafix:Disable.File"))
  def deleteGeneratedFolder(path: String): Task[Unit] = Task(new Directory(new File(path)).deleteRecursively())

  def dataFrameWriterBuilderSpec: Spec[SparkSession, TestFailure[Any], TestSuccess] =
    suite("DataFrameWriter Builder Spec")(
      test("DataFrameWriter can change its mode") {
        val mode = SaveMode.Append
        for {
          df <- read
          writer = df.write
        } yield assertTrue(writer.mode(mode).mode == mode)
      },
      test("DataFrameWriter can add multiple options") {
        val options =
          Map(
            "a" -> "b",
            "c" -> "d"
          )

        for {
          df <- read
          writer = df.write
        } yield assertTrue(writer.options(options).options == options)
      }
    )

  def dataFrameWriterSavingSpec: Spec[SparkSession, TestFailure[Any], TestSuccess] = {
    final case class WriterTest(
        extension: String,
        readAgain: String => Spark[DataFrame],
        write:     String => DataFrame => Task[Unit]
    ) {
      def build: Spec[SparkSession, TestFailure[Any], TestSuccess] =
        test(s"DataFrameWriter can save a DataFrame to $extension") {
          val path: String = s"$resourcesPath/output.$extension"

          val pipeline = Pipeline.buildWithoutTransformation(read)(write(path))

          for {
            _      <- pipeline.run
            df     <- readAgain(path)
            output <- df.count
            _      <- deleteGeneratedFolder(path)
          } yield assertTrue(output == 4L)
        }
    }

    val tests =
      List(
        WriterTest(
          extension = "csv",
          readAgain = path => readCsv(path),
          write     = path => _.write.withHeader.csv(path)
        ),
        WriterTest(
          extension = "parquet",
          readAgain = path => SparkSession.read.parquet(path),
          write     = path => _.write.parquet(path)
        ),
        WriterTest(
          extension = "json",
          readAgain = path => SparkSession.read.json(path),
          write     = path => _.write.json(path)
        )
      )

    suite("DataFrameWriter Saving Formats")(tests.map(_.build): _*)
  }

  def dataFrameWriterOptionDefinitionsSpec: Spec[SparkSession, TestFailure[Any], TestSuccess] = {
    final case class WriterTest(
        testName:      String,
        endo:          DataFrameWriter[Row] => DataFrameWriter[Row],
        expectedKey:   String,
        expectedValue: String
    ) {

      def build: Spec[SparkSession, TestFailure[Any], TestSuccess] =
        test(s"DataFrameWriter can add the option ($testName)") {
          for {
            df <- read
            write             = df.write
            writerWithOptions = endo(write)
            options           = Map(expectedKey -> expectedValue)
          } yield assertTrue(writerWithOptions.options == options)
        }
    }

    val tests =
      List(
        WriterTest(
          testName      = "Any option with a boolean value",
          endo          = _.option("a", value = true),
          expectedKey   = "a",
          expectedValue = "true"
        ),
        WriterTest(
          testName      = "Any option with a int value",
          endo          = _.option("a", 1),
          expectedKey   = "a",
          expectedValue = "1"
        ),
        WriterTest(
          testName      = "Any option with a float value",
          endo          = _.option("a", 1f),
          expectedKey   = "a",
          expectedValue = "1.0"
        ),
        WriterTest(
          testName      = "Any option with a double value",
          endo          = _.option("a", 1d),
          expectedKey   = "a",
          expectedValue = "1.0"
        ),
        WriterTest(
          testName      = "Option that read header",
          endo          = _.withHeader,
          expectedKey   = "header",
          expectedValue = "true"
        )
      )

    suite("DataFrameWriter Option Definitions")(tests.map(_.build): _*)
  }
}
