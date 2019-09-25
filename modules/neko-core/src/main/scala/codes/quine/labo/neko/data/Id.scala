package codes.quine.labo
package neko
package data

import scala.annotation.tailrec
import instances.string._, syntax._

final case class Id[+A](value: A) {
  def map[B](f: A => B): Id[B] = Id(f(value))

  def flatMap[B](f: A => Id[B]): Id[B] = f(value)
}

object Id extends IdInstances0

private[data] trait IdInstances0 extends IdInstances1 {
  implicit val idBimonadInstance: Bimonad[Id] = new Bimonad[Id] {
    def pure[A](a: A): Id[A] = Id(a)
    override def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = fa.flatMap(f)

    @tailrec
    def tailRecM[A, B](a: A)(f: A => Id[Either[A, B]]): Id[B] =
      f(a).value match {
        case Right(b) => Id(b)
        case Left(a0) => tailRecM(a0)(f)
      }

    override def map[A, B](fa: Id[A])(f: A => B): Id[B] = fa.map(f)

    def extract[A](fa: Id[A]): A = fa.value
    def coflatMap[A, B](fa: Id[A])(f: Id[A] => B): Id[B] = Id(f(fa))
  }

  implicit def idEqInstance[A: Eq]: Eq[Id[A]] = Eq[A].by(_.value)
  implicit def idPartialOrdInstance[A: PartialOrd]: PartialOrd[Id[A]] = PartialOrd[A].by(_.value)
  implicit def idOrdInstance[A: Ord]: Ord[Id[A]] = Ord[A].by(_.value)

  implicit def idSemigroupInstance[A: Semigroup]: Semigroup[Id[A]] = Semigroup[A].by(_.value, Id(_))
  implicit def idMonoidInstance[A: Monoid]: Monoid[Id[A]] = Monoid[A].by(_.value, Id(_))
}

private[data] trait IdInstances1 {
  implicit def idHashInstance[A: Hash]: Hash[Id[A]] = new Hash[Id[A]] {
    def eqv(x: Id[A], y: Id[A]): Boolean = x.value === y.value
    def hash(x: Id[A]): Int = "Id".hash * 31 + x.value.hash
  }
}