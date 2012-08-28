package rapture.html

// This is a mess.
trait HtmlDefs { this : Html5 =>

  class Stringable[T](val string: T => String)
  implicit val stringStringable = new Stringable[String](_.toString)
  implicit val symbolStringable = new Stringable[Symbol](_.name)
  implicit val booleanStringable = new Stringable[Boolean](v => if(v) "on" else "off")
  implicit val typeStringable = new Stringable[TypeOption](_.toString)

  trait AttributeType
  class Attribute[+AttributeType](val key : String, val value : String)
  
  trait BaseAttributeKey[T, +U <: AttributeType] {
    def key : String
    def ->(value : T)(implicit e: Stringable[T]): Attribute[U] = new Attribute[U](key, e.string(value))
  }

  class AttributeKey[T, +U <: AttributeType](k : String) extends BaseAttributeKey[T, U] {
    val key = k
  }

  abstract class BooleanAttributeKey[+U <: AttributeType](k : String) extends AttributeKey[Boolean, U](k) {
    def trueValue: String
    def falseValue: String
  }

  trait Element[+ElementType] {
    
    def serialize(sb : StringBuilder, n : Int, indent : Boolean) : Unit
    
    override def toString() = {
      val sb = new StringBuilder
      serialize(sb, 0, false)
      sb.toString
    }
  }

  implicit def stringToElement(s : String) = new Element[Text] {
    override def serialize(sb : StringBuilder, n : Int, indent : Boolean) = {
      if(indent) sb.append("  "*n)
      sb.append(s.replaceAll("&", "&amp;").replaceAll("<", "&lt;"))
    }
  }

  implicit def tagToEmptyElement[T <: ElementType, S <: ElementType, Q <: AttributeType]
      (t : Tag[T, S, Q]) : Element[S] = t.build(Nil, Nil)

  class TransparentTag[X <: ElementType, AT <: AttributeType](name : String, block : Boolean = true)
      extends Tag[X, X, AT](name, block)

  trait AttributedElement[ThisType <: ElementType, ChildType <: ElementType] {
    def apply(body : Element[ChildType]*) : Element[ThisType]
  }

  implicit def convert[ThisType <: ElementType, ChildType <: ElementType]
      (f : AttributedElement[ThisType, ChildType]) : Element[ThisType] = f(Nil : _*)

  class Tag[ChildType <: ElementType, ThisType <: ElementType, AT <: AttributeType]
      (val name : String, block : Boolean = true) {
    
    def apply(attributes : Attribute[AT]*) = new AttributedElement[ThisType, ChildType] {
      def apply(body : Element[ChildType]*) : Element[ThisType] =
        build(attributes.toList, body.toList)
    }
    def apply(body : Element[ChildType]*) : Element[ThisType] = build(Nil, body.toList)
    
    def build(attributes : List[Attribute[AT]], body : List[Element[ChildType]]) :
        Element[ThisType] = new Element[ThisType] {
      def serialize(sb : StringBuilder, n : Int, indent : Boolean) =
        doSerialize(block, name, attributes map { a => (a.key, a.value) } toMap, body, sb, n,
            indent)
    }
  }
}

