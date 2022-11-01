import kotlinx.html.TagConsumer
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.svg.SVGElement

abstract class AppScreen(protected val app: IController, protected val root: HTMLElement)

interface Renderable {
    fun render(tc: TagConsumer<HTMLElement>)
}

interface RenderableInto {
    fun render(parent: HTMLElement)
}

abstract class Component {
    protected var root: Element? = null
}

abstract class ActivateableComponent: Component() {
    private var isActive: Boolean = false
        set (value) {
            field = value
            if (value) {
                root?.classList?.add("active")
            } else root?.classList?.remove("active")
        }
}