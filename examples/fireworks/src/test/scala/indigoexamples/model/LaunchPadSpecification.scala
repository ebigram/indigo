package indigoexamples.model

import org.scalacheck._

import indigo.Dice
import ingidoexamples.model.LaunchPad
import indigo.shared.datatypes.Point
import indigo.EqualTo._
import indigo.shared.datatypes.Rectangle

class LaunchPadSpecification extends Properties("LaunchPad") {

  import Generators._

  def launchPadGen: Gen[LaunchPad] =
    diceGen.map(dice => LaunchPad.generateLaunchPad(dice))

  property("generate a launch pad with a timer up to 1.5 seconds") = Prop.forAll(launchPadGen) { launchPad =>
    launchPad.countDown.value >= 1 && launchPad.countDown.value <= 1500
  }

  property("generate a launch pad vertex y=0 and x=0 to 1") = Prop.forAll(diceGen) { dice =>
    val launchPad: LaunchPad =
      LaunchPad.generateLaunchPad(dice)

    launchPad.position.y === 0 && launchPad.position.x >= 0 && launchPad.position.x <= 1
  }

}
