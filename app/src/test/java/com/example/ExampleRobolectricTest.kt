package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ProblemSolution
import com.example.data.model.Scene3DJsonCodec
import com.example.util.Scene3DSynthesizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("انیشتین همراته", appName)
  }

  @Test
  fun `scene3d json codec roundtrips cleanly`() {
    val sol = ProblemSolution(
      subject = "شیمی",
      topic = "حلقه بنزن",
      identifiedProblem = "ساختار بنزن را رسم کنید",
      finalAnswer = "شش‌ضلعی مسطح با زاویه ۱۲۰ درجه"
    )
    val scene = Scene3DSynthesizer.resolveScene(sol)
    val json = Scene3DJsonCodec.toJsonString(scene)
    val decoded = Scene3DJsonCodec.fromJsonString(json)

    assertNotNull(decoded)
    assertEquals(scene.sceneType, decoded!!.sceneType)
    assertEquals(scene.shapeVariant, decoded.shapeVariant)
    assertEquals(scene.nodes.size, decoded.nodes.size)
    assertEquals(scene.edges.size, decoded.edges.size)
  }
}
