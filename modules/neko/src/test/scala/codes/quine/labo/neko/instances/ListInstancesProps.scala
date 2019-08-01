package codes.quine.labo.neko
package instances

import props._

import scalaprops._

object ListInstancesProps extends Scalaprops {
  val laws = Properties.list(
    EqProps[List[Int]].eq,
    FunctorProps[List].functor[Int, Int, Int],
    ApplicativeProps[List].applicative[Int, Int, Int],
    MonadProps[List].monad[Int, Int, Int],
    SemigroupKProps[List].semigroupK[Int],
    MonoidKProps[List].monoidK[Int],
    AlternativeProps[List].alternative[Int, Int]
  )
}
