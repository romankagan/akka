package akka.http.model
package headers

import akka.http.util._

case class HttpChallenge(scheme: String, realm: String,
                         params: Map[String, String] = Map.empty) extends ValueRenderable {

  def render[R <: Rendering](r: R): r.type = {
    r ~~ scheme ~~ " realm=" ~~# realm
    if (params.nonEmpty) params.foreach { case (k, v) ⇒ r ~~ ',' ~~ k ~~ '=' ~~# v }
    r
  }
}
