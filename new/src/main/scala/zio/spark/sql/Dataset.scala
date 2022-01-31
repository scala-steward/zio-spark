package zio.spark.sql

import org.apache.spark.sql.{Dataset => UnderlyingDataset, Encoder}

import zio.Task

final case class Dataset[T](ds: UnderlyingDataset[T]) {

  /**
   * Maps each record to the specified type.
   *
   * See [[UnderlyingDataset.as]] for more information.
   */
  def as[U: Encoder]: Dataset[U] = transformation(_.as[U])

  /** Applies a transformation to the underlying dataset. */
  def transformation[U](f: UnderlyingDataset[T] => UnderlyingDataset[U]): Dataset[U] = Dataset(f(ds))

  /**
   * Limits the number of rows of a dataset.
   *
   * See [[UnderlyingDataset.limit]] for more information.
   */
  def limit(n: Int): Dataset[T] = transformation(_.limit(n))

  /**
   * Applies the function f to each record of the dataset.
   *
   * See [[UnderlyingDataset.map]] for more information.
   */
  def map[U: Encoder](f: T => U): Dataset[U] = transformation(_.map(f))

  /**
   * Applies the function f to each record of the dataset and then
   * flattening the result.
   *
   * See [[UnderlyingDataset.flatMap]] for more information.
   */
  def flatMap[U: Encoder](f: T => Iterable[U]): Dataset[U] = transformation(_.flatMap(f))

  /**
   * Counts the number of rows of a dataset.
   *
   * See [[UnderlyingDataset.count]] for more information.
   */
  def count: Task[Long] = action(_.count())

  /**
   * Retrieves the rows of a dataset as a list of elements.
   *
   * See [[UnderlyingDataset.collect]] for more information.
   */
  def collect: Task[List[T]] = action(_.collect().toList)

  /** Alias for [[head]]. */
  def first: Task[T] = head

  /**
   * Takes the first element of a dataset.
   *
   * See [[UnderlyingDataset.head]] for more information.
   */
  def head: Task[T] = head(1).map(_.head)

  /**
   * Takes the n elements of a dataset.
   *
   * See [[UnderlyingDataset.head]] for more information.
   */
  def head(n: Int): Task[List[T]] = action(_.head(n).toList)

  /** Alias for [[head]]. */
  def take(n: Int): Task[List[T]] = head(n)

  /** Alias for [[headOption]]. */
  def firstOption: Task[Option[T]] = headOption

  /** Takes the first element of a dataset or None. */
  def headOption: Task[Option[T]] = head(1).map(_.headOption)

  /** Alias for [[tail]]. */
  def last: Task[T] = tail

  /**
   * Takes the last element of a dataset or throws an exception.
   *
   * See [[UnderlyingDataset.tail]] for more information.
   */
  def tail: Task[T] = tail(1).map(_.head)

  /** Alias for [[tailOption]]. */
  def lastOption: Task[Option[T]] = tailOption

  /** Takes the last element of a dataset or None. */
  def tailOption: Task[Option[T]] = tail(1).map(_.headOption)

  /** Alias for [[tail]]. */
  def takeRight(n: Int): Task[List[T]] = tail(n)

  /**
   * Takes the n last elements of a dataset.
   *
   * See [[UnderlyingDataset.tail]] for more information.
   */
  def tail(n: Int): Task[List[T]] = action(_.tail(n).toList)

  /** Applies an action to the underlying dataset. */
  def action[A](f: UnderlyingDataset[T] => A): Task[A] = Task.attemptBlocking(f(ds))

}