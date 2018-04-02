package com.purplekingdomgames.indigo.gameengine.scenegraph

import com.purplekingdomgames.indigo.gameengine.assets.{AnimationStates, FontRegister}
import com.purplekingdomgames.indigo.gameengine.events.{GameEvent, ViewEvent}
import com.purplekingdomgames.indigo.gameengine.GameTime
import com.purplekingdomgames.indigo.gameengine.scenegraph.AnimationAction._
import com.purplekingdomgames.indigo.gameengine.scenegraph.datatypes._

object SceneGraphNode {
  def empty: Group = Group(Point.zero, Depth.Base, Nil)
}

sealed trait SceneGraphNode {
  def bounds: Rectangle
  val depth: Depth

  def withDepth(depth: Depth): SceneGraphNode
  def moveTo(pt: Point): SceneGraphNode
  def moveTo(x: Int, y: Int): SceneGraphNode
  def moveBy(pt: Point): SceneGraphNode
  def moveBy(x: Int, y: Int): SceneGraphNode

  private[gameengine] def flatten: List[Renderable] = {
    def rec(acc: List[Renderable]): List[Renderable] = {
      this match {
        case l: Renderable =>
          l :: acc

        case b: Group =>
          b.children
            .map(c => c.withDepth(c.depth + b.depth).moveBy(b.positionOffset))
            .flatMap(n => n.flatten) ++ acc
      }
    }

    rec(Nil)
  }

}

case class Group(positionOffset: Point, depth: Depth, children: List[SceneGraphNode]) extends SceneGraphNode {

  def withDepth(depth: Depth): SceneGraphNode =
    this.copy(depth = depth)

  def moveTo(pt: Point): Group =
    this.copy(positionOffset = pt)
  def moveTo(x: Int, y: Int): Group =
    moveTo(Point(x, y))

  def moveBy(pt: Point): Group =
    this.copy(
      positionOffset = this.positionOffset + pt
    )
  def moveBy(x: Int, y: Int): Group =
    moveBy(Point(x, y))

  def bounds: Rectangle =
    children match {
      case Nil =>
        Rectangle.zero

      case x :: xs =>
        xs.foldLeft(x.bounds) { (acc, node) =>
          Rectangle.enclosing(acc, node.bounds)
        }
    }

  def addChild(child: SceneGraphNode): Group =
    this.copy(children = children :+ child)

  def addChildren(additionalChildren: List[SceneGraphNode]): Group =
    this.copy(children = children ++ additionalChildren)

}

object Group {
  def apply(position: Point, depth: Depth, children: SceneGraphNode*): Group =
    Group(position, depth, children.toList)

  def apply(children: SceneGraphNode*): Group =
    Group(Point.zero, Depth.Base, children.toList)

  def apply(children: List[SceneGraphNode]): Group =
    Group(Point.zero, Depth.Base, children)
}

sealed trait Renderable extends SceneGraphNode {
  val bounds: Rectangle
  val imageAssetRef: String
  val effects: Effects
  val ref: Point
  val crop: Rectangle
  val eventHandler: ((Rectangle, GameEvent)) => Option[ViewEvent]

  private[gameengine] def frameHash: String

  def x: Int = bounds.position.x - ref.x
  def y: Int = bounds.position.y - ref.y

  def moveTo(pt: Point): Renderable
  def moveTo(x: Int, y: Int): Renderable

  def moveBy(pt: Point): Renderable
  def moveBy(x: Int, y: Int): Renderable

  def withDepth(depth: Depth): Renderable
  def withAlpha(a: Double): Renderable
  def withTint(red: Double, green: Double, blue: Double): Renderable
  def flipHorizontal(h: Boolean): Renderable
  def flipVertical(v: Boolean): Renderable

  def onEvent(e: ((Rectangle, GameEvent)) => Option[ViewEvent]): Renderable

  private[gameengine] val eventHandlerWithBoundsApplied: GameEvent => Option[ViewEvent]

  private[gameengine] def saveAnimationMemento: Option[AnimationMemento]

  private[gameengine] def applyAnimationMemento(animationStates: AnimationStates): Renderable

  private[gameengine] def runAnimationActions(gameTime: GameTime): Renderable
}

case class Graphic(bounds: Rectangle, depth: Depth, imageAssetRef: String, ref: Point, crop: Rectangle, effects: Effects, eventHandler: ((Rectangle, GameEvent)) => Option[ViewEvent]) extends Renderable {

  private[gameengine] def frameHash: String = crop.hash + "_" + imageAssetRef

  def moveTo(pt: Point): Graphic =
    this.copy(bounds = bounds.copy(position = pt))
  def moveTo(x: Int, y: Int): Graphic =
    moveTo(Point(x, y))

  def moveBy(pt: Point): Graphic =
    this.copy(bounds =
      bounds.copy(
        position = this.bounds.position + pt
      )
    )
  def moveBy(x: Int, y: Int): Graphic =
    moveBy(Point(x, y))

  def withDepth(depth: Depth): Graphic =
    this.copy(depth = depth)

  def withAlpha(a: Double): Graphic =
    this.copy(effects = effects.copy(alpha = a))

  def withTint(tint: Tint): Graphic =
    this.copy(effects = effects.copy(tint = tint))

  def withTint(red: Double, green: Double, blue: Double): Graphic =
    this.copy(effects = effects.copy(tint = Tint(red, green, blue)))

  def flipHorizontal(h: Boolean): Graphic =
    this.copy(effects = effects.copy(flip = Flip(horizontal = h, vertical = effects.flip.vertical)))

  def flipVertical(v: Boolean): Graphic =
    this.copy(effects = effects.copy(flip = Flip(horizontal = effects.flip.horizontal, vertical = v)))

  def withRef(ref: Point): Graphic =
    this.copy(ref = ref)
  def withRef(x: Int, y: Int): Graphic =
    this.copy(ref = Point(x, y))

  def withCrop(crop: Rectangle): Graphic =
    this.copy(crop = crop)
  def withCrop(x: Int, y: Int, width: Int, height: Int): Graphic =
    this.copy(crop = Rectangle(x, y, width, height))

  def onEvent(e: ((Rectangle, GameEvent)) => Option[ViewEvent]): Graphic =
    this.copy(eventHandler = e)

  private[gameengine] val eventHandlerWithBoundsApplied: GameEvent => Option[ViewEvent] =
    (e: GameEvent) => eventHandler((bounds, e))

  private[gameengine] def applyAnimationMemento(animationStates: AnimationStates): Graphic = this

  private[gameengine] def saveAnimationMemento: Option[AnimationMemento] = None

  private[gameengine] def runAnimationActions(gameTime: GameTime): Graphic = this

}

object Graphic {
  def apply(x: Int, y: Int, width: Int, height: Int, depth: Int, imageAssetRef: String): Graphic =
    Graphic(
      bounds = Rectangle(x, y, width, height),
      depth = depth,
      imageAssetRef = imageAssetRef,
      ref = Point.zero,
      crop = Rectangle(0, 0, width, height),
      effects = Effects.default,
      eventHandler = (_:(Rectangle, GameEvent)) => None
    )

  def apply(bounds: Rectangle, depth: Int, imageAssetRef: String): Graphic =
    Graphic(
      bounds = bounds,
      depth = depth,
      imageAssetRef = imageAssetRef,
      ref = Point.zero,
      crop = bounds,
      effects = Effects.default,
      eventHandler = (_:(Rectangle, GameEvent)) => None
    )
}

case class Sprite(bindingKey: BindingKey, bounds: Rectangle, depth: Depth, imageAssetRef: String, animations: Animations, ref: Point, effects: Effects, eventHandler: ((Rectangle, GameEvent)) => Option[ViewEvent]) extends Renderable {

  private[gameengine] def frameHash: String = animations.currentFrame.bounds.hash + "_" + imageAssetRef

  def withDepth(depth: Depth): Sprite =
    this.copy(depth = depth)

  val crop: Rectangle = bounds

  def moveTo(pt: Point): Sprite =
    this.copy(bounds = bounds.copy(position = pt))
  def moveTo(x: Int, y: Int): Sprite =
    moveTo(Point(x, y))

  def moveBy(pt: Point): Sprite =
    this.copy(bounds =
      bounds.copy(
        position = this.bounds.position + pt
      )
    )
  def moveBy(x: Int, y: Int): Sprite =
    moveBy(Point(x, y))

  def withBindingKey(bindingKey: BindingKey): Sprite =
    this.copy(bindingKey = bindingKey)

  def withAlpha(a: Double): Sprite =
    this.copy(effects = effects.copy(alpha = a))

  def withTint(tint: Tint): Sprite =
    this.copy(effects = effects.copy(tint = tint))

  def withTint(red: Double, green: Double, blue: Double): Sprite =
    this.copy(effects = effects.copy(tint = Tint(red, green, blue)))

  def flipHorizontal(h: Boolean): Sprite =
    this.copy(effects = effects.copy(flip = Flip(horizontal = h, vertical = effects.flip.vertical)))

  def flipVertical(v: Boolean): Sprite =
    this.copy(effects = effects.copy(flip = Flip(horizontal = effects.flip.horizontal, vertical = v)))

  def withRef(ref: Point): Sprite =
    this.copy(ref = ref)
  def withRef(x: Int, y: Int): Sprite =
    this.copy(ref = Point(x, y))

  def play(): Sprite =
    this.copy(animations = animations.addAction(Play))

  def changeCycle(label: String): Sprite =
    this.copy(animations = animations.addAction(ChangeCycle(label)))

  def jumpToFirstFrame(): Sprite =
    this.copy(animations = animations.addAction(JumpToFirstFrame))

  def jumpToLastFrame(): Sprite =
    this.copy(animations = animations.addAction(JumpToLastFrame))

  def jumpToFrame(number: Int): Sprite =
    this.copy(animations = animations.addAction(JumpToFrame(number)))

  def onEvent(e: ((Rectangle, GameEvent)) => Option[ViewEvent]): Sprite =
    this.copy(eventHandler = e)

  private[gameengine] val eventHandlerWithBoundsApplied: GameEvent => Option[ViewEvent] =
    (e: GameEvent) => eventHandler((bounds, e))

  private[gameengine] def saveAnimationMemento: Option[AnimationMemento] = Option(animations.saveMemento(bindingKey))

  private[gameengine] def applyAnimationMemento(animationStates: AnimationStates): Sprite =
    animationStates.findStateWithBindingKey(bindingKey) match {
      case Some(memento) => this.copy(animations = animations.applyMemento(memento))
      case None => this
    }

  private[gameengine] def runAnimationActions(gameTime: GameTime): Sprite = this.copy(animations = animations.runActions(gameTime))

}

object Sprite {
  def apply(bindingKey: BindingKey, x: Int, y: Int, width: Int, height: Int, depth: Int, imageAssetRef: String, animations: Animations): Sprite =
    Sprite(
      bindingKey = bindingKey,
      bounds = Rectangle(x, y, width, height),
      depth = depth,
      imageAssetRef = imageAssetRef,
      animations = animations,
      ref = Point.zero,
      effects = Effects.default,
      eventHandler = (_:(Rectangle, GameEvent)) => None
    )
}

//TODO: FontInfo can be very large and is embedded into every instance, find a way to look up by reference.
case class Text(text: String, alignment: TextAlignment, position: Point, depth: Depth, fontKey: FontKey, effects: Effects, eventHandler: ((Rectangle, GameEvent)) => Option[ViewEvent]) extends Renderable {

  private[gameengine] def frameHash: String = "" // Not used - look up done another way.

  // Handled a different way
  val ref: Point = Point(0, 0)

  def lines: List[TextLine] =
    FontRegister.findByFontKey(fontKey).map { fontInfo =>
      text
        .split('\n').toList
        .map(_.replace("\n", ""))
        .map(line => TextLine(line, Text.calculateBoundsOfLine(line, fontInfo)))
    }.getOrElse(Nil)

  val bounds: Rectangle =
    lines.map(_.lineBounds).fold(Rectangle.zero) {
      (acc, next) => acc.copy(size = Point(Math.max(acc.width, next.width), acc.height + next.height))
    }

  val crop: Rectangle = bounds

  // Handled a different way during conversion to DisplayObject
  val imageAssetRef: String = ""

  def moveTo(pt: Point): Text =
    this.copy(position = pt)
  def moveTo(x: Int, y: Int): Text =
    moveTo(Point(x, y))

  def moveBy(pt: Point): Text =
    this.copy(
      position = this.position + pt
    )
  def moveBy(x: Int, y: Int): Text =
    moveBy(Point(x, y))

  def withDepth(depth: Depth): Text =
    this.copy(depth = depth)

  def withAlpha(a: Double): Text =
    this.copy(effects = effects.copy(alpha = a))

  def withTint(tint: Tint): Text =
    this.copy(effects = effects.copy(tint = tint))

  def withTint(red: Double, green: Double, blue: Double): Text =
    this.copy(effects = effects.copy(tint = Tint(red, green, blue)))

  def flipHorizontal(h: Boolean): Text =
    this.copy(effects = effects.copy(flip = Flip(horizontal = h, vertical = effects.flip.vertical)))

  def flipVertical(v: Boolean): Text =
    this.copy(effects = effects.copy(flip = Flip(horizontal = effects.flip.horizontal, vertical = v)))

  def withAlignment(alignment: TextAlignment): Text =
    this.copy(alignment = alignment)

  def alignLeft: Text = copy(alignment = AlignLeft)
  def alignCenter: Text = copy(alignment = AlignCenter)
  def alignRight: Text = copy(alignment = AlignRight)

  def withText(text: String): Text =
    this.copy(text = text)

  def withFontKey(fontKey: FontKey): Text =
    this.copy(fontKey = fontKey)

  def onEvent(e: ((Rectangle, GameEvent)) => Option[ViewEvent]): Text =
    this.copy(eventHandler = e)

  private val realBound: Rectangle =
    (alignment, bounds.copy(position = position)) match {
      case (AlignLeft, b) => b
      case (AlignCenter, b) => b.copy(position = Point(b.x - (b.width / 2), b.y))
      case (AlignRight, b) => b.copy(position = Point(b.x - b.width, b.y))
    }

  private[gameengine] val eventHandlerWithBoundsApplied: GameEvent => Option[ViewEvent] =
    (e: GameEvent) => eventHandler((realBound, e))

  private[gameengine] def applyAnimationMemento(animationStates: AnimationStates): Text = this

  private[gameengine] def saveAnimationMemento: Option[AnimationMemento] = None

  private[gameengine] def runAnimationActions(gameTime: GameTime): Text = this

}

case class TextLine(text: String, lineBounds: Rectangle)

object Text {

  def calculateBoundsOfLine(lineText: String, fontInfo: FontInfo): Rectangle = {
    lineText.toList
      .map(c => fontInfo.findByCharacter(c).bounds)
      .fold(Rectangle.zero)((acc, curr) => Rectangle(0, 0, acc.width + curr.width, Math.max(acc.height, curr.height)))
  }

  def apply(text: String, x: Int, y: Int, depth: Int, fontKey: FontKey): Text =
    Text(
      text = text,
      alignment = AlignLeft,
      position = Point(x, y),
      depth = depth,
      fontKey = fontKey,
      effects = Effects.default,
      eventHandler = (_:(Rectangle, GameEvent)) => None
    )
}
