/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mu.rpc.http

import cats.effect._
import fs2.Stream
import io.circe.generic.auto._
import io.circe.syntax._
import mu.rpc.http.Utils._
import org.http4s._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.dsl.io._

class UnaryGreeterRestClient[F[_]: Sync](uri: Uri) {

  private implicit val responseDecoder: EntityDecoder[F, HelloResponse] = jsonOf[F, HelloResponse]

  def getHello()(implicit client: Client[F]): F[HelloResponse] = {
    val request = Request[F](Method.GET, uri / "getHello")
    client.expectOr[HelloResponse](request)(handleResponseError)
  }

  def sayHello(arg: HelloRequest)(implicit client: Client[F]): F[HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHello")
    client.expectOr[HelloResponse](request.withEntity(arg.asJson))(handleResponseError)
  }

}

class Fs2GreeterRestClient[F[_]: Sync](uri: Uri) {

  private implicit val responseDecoder: EntityDecoder[F, HelloResponse] = jsonOf[F, HelloResponse]

  def sayHellos(arg: Stream[F, HelloRequest])(implicit client: Client[F]): F[HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHellos")
    client.expectOr[HelloResponse](request.withEntity(arg.map(_.asJson)))(handleResponseError)
  }

  def sayHelloAll(arg: HelloRequest)(implicit client: Client[F]): Stream[F, HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHelloAll")
    client.stream(request.withEntity(arg.asJson)).flatMap(_.asStream[HelloResponse])
  }

  def sayHellosAll(arg: Stream[F, HelloRequest])(
      implicit client: Client[F]): Stream[F, HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHellosAll")
    client.stream(request.withEntity(arg.map(_.asJson))).flatMap(_.asStream[HelloResponse])
  }

}

class MonixGreeterRestClient[F[_]: ConcurrentEffect](uri: Uri)(
    implicit sc: monix.execution.Scheduler) {

  import monix.reactive.Observable
  import mu.rpc.http.Utils._

  private implicit val responseDecoder: EntityDecoder[F, HelloResponse] = jsonOf[F, HelloResponse]

  def sayHellos(arg: Observable[HelloRequest])(implicit client: Client[F]): F[HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHellos")
    client.expectOr[HelloResponse](request.withEntity(arg.toFs2Stream.map(_.asJson)))(
      handleResponseError)
  }

  def sayHelloAll(arg: HelloRequest)(implicit client: Client[F]): Observable[HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHelloAll")
    client.stream(request.withEntity(arg.asJson)).flatMap(_.asStream[HelloResponse]).toObservable
  }

  def sayHellosAll(arg: Observable[HelloRequest])(
      implicit client: Client[F]): Observable[HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHellosAll")
    client
      .stream(request.withEntity(arg.toFs2Stream.map(_.asJson)))
      .flatMap(_.asStream[HelloResponse])
      .toObservable
  }

}