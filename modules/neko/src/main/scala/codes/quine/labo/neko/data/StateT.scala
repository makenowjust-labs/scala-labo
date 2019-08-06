package codes.quine.labo
package neko
package data

import syntax._

final case class StateT[F[_], S, A](run: S => F[(S, A)]) {
  def map[B](f: A => B)(implicit F: Functor[F]): StateT[F, S, B] =
    StateT(s => run(s).map { case (s1, a) => (s1, f(a)) })

  def flatMap[B](f: A => StateT[F, S, B])(implicit F: FlatMap[F]): StateT[F, S, B] =
    StateT { s =>
      for {
        (s1, a) <- run(s)
        (s2, b) <- f(a).run(s1)
      } yield (s2, b)
    }
}

object StateT extends StateTInstances0 {
  def run[F[_], S, A](fa: StateT[F, S, A])(s: S): F[(S, A)] = fa.run(s)
  def exec[F[_]: Functor, S, A](fa: StateT[F, S, A])(s: S): F[S] = run(fa)(s).map(_._1)
  def eval[F[_]: Functor, S, A](fa: StateT[F, S, A])(s: S): F[A] = run(fa)(s).map(_._2)

  def get[F[_], S](implicit F: Applicative[F]): StateT[F, S, S] = StateT(s => F.pure((s, s)))
  def put[F[_], S](s: S)(implicit F: Applicative[F]): StateT[F, S, Unit] = StateT(_ => F.pure((s, ())))
  def modify[F[_], S](f: S => S)(implicit F: Applicative[F]): StateT[F, S, Unit] = StateT(s => F.pure((f(s), ())))
}

private[data] trait StateTInstances0 extends StateTInstances1 {
  implicit def stateTFunctorInstance[F[_]: Functor, S]: Functor[StateT[F, S, *]] = new Functor[StateT[F, S, *]] {
    def map[A, B](fa: StateT[F, S, A])(f: A => B): StateT[F, S, B] = fa.map(f)
  }

  implicit def stateTMonadInstance[F[_]: Monad, S]: Monad[StateT[F, S, *]] = new Monad[StateT[F, S, *]] {
    def pure[A](a: A): StateT[F, S, A] = StateT(s => Monad[F].pure((s, a)))
    override def map[A, B](fa: StateT[F, S, A])(f: A => B): StateT[F, S, B] = fa.map(f)
    override def flatMap[A, B](fa: StateT[F, S, A])(f: A => StateT[F, S, B]): StateT[F, S, B] = fa.flatMap(f)

    def tailRecM[A, B](a: A)(f: A => StateT[F, S, Either[A, B]]): StateT[F, S, B] =
      StateT { s =>
        Monad[F].tailRecM[(S, A), (S, B)]((s, a)) {
          case (s0, a0) =>
            f(a0).run(s0).map {
              case (s1, Left(a1)) => Left((s1, a1))
              case (s1, Right(b)) => Right((s1, b))
            }
        }
      }
  }
}

private[data] trait StateTInstances1 {
  implicit def stateTAlternativeInstance[F[_]: Alternative, S](implicit F: Monad[F]): Alternative[StateT[F, S, *]] =
    new Alternative[StateT[F, S, *]] {
      def pure[A](a: A): StateT[F, S, A] = StateT(s => Monad[F].pure((s, a)))
      override def map[A, B](fa: StateT[F, S, A])(f: A => B): StateT[F, S, B] = fa.map(f)(F)

      def ap[A, B](ff: StateT[F, S, A => B])(fa: StateT[F, S, A]): StateT[F, S, B] =
        ff.flatMap(f => fa.map(f)(F))

      def emptyK[A]: StateT[F, S, A] = StateT(_ => Alternative[F].emptyK)
      def concatK[A](x: StateT[F, S, A], y: StateT[F, S, A]): StateT[F, S, A] =
        StateT(s => x.run(s) <+> y.run(s))
    }
}
