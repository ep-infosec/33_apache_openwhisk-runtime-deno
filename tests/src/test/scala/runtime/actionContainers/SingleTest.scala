/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package runtime.actionContainers

import actionContainers.ActionContainer.withContainer
import actionContainers.{ActionContainer, ActionProxyContainerTestUtils}
import common.WskActorSystem
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import spray.json.{JsArray, JsObject, JsString, JsonParser}

@RunWith(classOf[JUnitRunner])
class SingleTest extends ActionProxyContainerTestUtils with WskActorSystem {
  lazy val imageName = "action-deno-v1.3.0"

  def withActionContainer(env: Map[String, String] = Map.empty)(code: ActionContainer => Unit) = {
    withContainer(imageName, env)(code)
  }

  behavior of imageName

  val code =
    """|export default (args: any) => {
       |   console.log("hello stdout");
       |   console.error("hello stderr")
       |   return args
       |}
       |""".stripMargin

  val data = JsonParser("""{"name":"Jane"}""")

  it should "return an echo of the input" in {
    val (out, err) = withActionContainer() { c =>
      val (initCode, _) = c.init(initPayload(code))
      initCode should be(200)
      val (runCode, runRes) = c.run(runPayload(data))
      runCode should be(200)
    }
  }

  it should "support return array result" in {
    val helloArrayCode =
      """|export default (args: any) => {
         |   return ["a", "b"]
         |}
         |""".stripMargin
    val (out, err) = withActionContainer() { c =>
      val (initCode, _) = c.init(initPayload(helloArrayCode))
      initCode should be(200)
      val (runCode, runRes) = c.runForJsArray(runPayload(JsObject()))
      runCode should be(200)
      runRes shouldBe Some(JsArray(JsString("a"), JsString("b")))
    }
  }

  it should "support array as input param" in {
    val helloArrayCode =
      """|export default (args: any) => {
         |   return args
         |}
         |""".stripMargin
    val (out, err) = withActionContainer() { c =>
      val (initCode, _) = c.init(initPayload(helloArrayCode))
      initCode should be(200)
      val (runCode, runRes) = c.runForJsArray(runPayload(JsArray(JsString("a"), JsString("b"))))
      runCode should be(200)
      runRes shouldBe Some(JsArray(JsString("a"), JsString("b")))
    }
  }
}
