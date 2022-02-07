package dev.fritz2.headless.components

import dev.fritz2.binding.Store
import dev.fritz2.binding.storeOf
import dev.fritz2.dom.Listener
import dev.fritz2.dom.Tag
import dev.fritz2.dom.html.*
import dev.fritz2.headless.foundation.*
import dev.fritz2.headless.validation.ComponentValidationMessage
import dev.fritz2.identification.Id
import kotlinx.browser.document
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.w3c.dom.*


class RadioGroup<C : HTMLElement, T>(tag: Tag<C>, private val explicitId: String?) :
    Tag<C> by tag {

    private var label: Tag<HTMLElement>? = null
    private var validationMessages: Tag<HTMLElement>? = null

    val componentId: String by lazy { explicitId ?: value.id ?: Id.next() }
    private val isActive: Store<T?> = storeOf(null)
    val value = DatabindingProperty<T>()

    private var withKeyboardNavigation = true

    var options: List<T> = emptyList()


    fun render() {
        attr("id", componentId)
        attr("role", Aria.Role.radiogroup)
        attr(Aria.invalid, "true".whenever(value.hasError))
        label?.let { attr(Aria.labelledby, it.id) }
        if (withKeyboardNavigation == true) {
            value.handler?.invoke(
                value.data.flatMapLatest { option ->
                    keydowns.mapNotNull { event ->
                        when (shortcutOf(event)) {
                            Keys.ArrowDown -> options.rotateNext(option)
                            Keys.ArrowUp -> options.rotatePrevious(option)
                            else -> null
                        }.also {
                            if (it != null) {
                                event.stopImmediatePropagation()
                                event.preventDefault()
                                isActive.update(it)
                            }
                        }
                    }
                })
        }
    }

    fun <CL : HTMLElement> RenderContext.radioGroupLabel(
        classes: String? = null,
        scope: (ScopeContext.() -> Unit) = {},
        tag: TagFactory<Tag<CL>>,
        content: Tag<CL>.() -> Unit
    ) = tag(this, classes, "$componentId-label", scope, content).also { label = it }

    fun RenderContext.radioGroupLabel(
        classes: String? = null,
        scope: (ScopeContext.() -> Unit) = {},
        content: Tag<HTMLLabelElement>.() -> Unit
    ) = radioGroupLabel(classes, scope, RenderContext::label, content)

    fun <CV : HTMLElement> RenderContext.radioGroupValidationMessages(
        classes: String? = null,
        scope: (ScopeContext.() -> Unit) = {},
        tag: TagFactory<Tag<CV>>,
        content: Tag<CV>.(List<ComponentValidationMessage>) -> Unit
    ) = value.validationMessages.render { messages ->
        if (messages.isNotEmpty()) {
            tag(this, classes, "$componentId-validation-messages", scope, { })
                .apply {
                    content(messages)
                }.also { validationMessages = it }
        }
    }

    fun RenderContext.radioGroupValidationMessages(
        classes: String? = null,
        scope: (ScopeContext.() -> Unit) = {},
        content: Tag<HTMLDivElement>.(List<ComponentValidationMessage>) -> Unit
    ) = radioGroupValidationMessages(classes, scope, RenderContext::div, content)

    inner class RadioGroupOption<CO : HTMLElement>(
        tag: Tag<CO>,
        private val option: T,
        id: String?
    ) : Tag<CO> by tag {

        val selected = value.data.map { it == option }
        val active = isActive.data.map { it == option }.distinctUntilChanged()

        private var toggle: Tag<HTMLElement>? = null
        private var label: Tag<HTMLElement>? = null
        private var descriptions: MutableList<Tag<HTMLElement>> = mutableListOf()

        val optionId = "$componentId-${id ?: Id.next()}"

        fun render() {
            toggle?.apply {
                label?.let { attr(Aria.labelledby, it.id) }
                attr(
                    Aria.describedby,
                    value.validationMessages.map { messages ->
                        if (messages.isNotEmpty()) validationMessages?.id
                        else descriptions.map { it.id }.joinToString(" ")
                    }
                )
            }
        }

        fun <CT : HTMLElement> RenderContext.radioGroupOptionToggle(
            classes: String? = null,
            scope: (ScopeContext.() -> Unit) = {},
            tag: TagFactory<Tag<CT>>,
            content: Tag<CT>.() -> Unit
        ) = tag(this, classes, optionId, scope) {
            content()
            attr("role", Aria.Role.radio)
            attr(Aria.checked, selected.asString())
            attr("tabindex", selected.map { if (it) "0" else "-1" })
            var toggleEvent: Listener<*, *> = clicks
            if (domNode is HTMLInputElement) {
                if (domNode.getAttribute("name") == null) {
                    attr("name", componentId)
                }
                withKeyboardNavigation = false
                toggleEvent = changes
            }
            value.handler?.invoke(toggleEvent.map { option })
            active handledBy {
                if (it && domNode != document.activeElement) {
                    domNode.focus()
                }
            }
            focuss.map { option } handledBy isActive.update
            blurs.map { null } handledBy isActive.update
        }.also { toggle = it }

        fun RenderContext.radioGroupOptionToggle(
            classes: String? = null,
            scope: (ScopeContext.() -> Unit) = {},
            content: Tag<HTMLDivElement>.() -> Unit
        ) = radioGroupOptionToggle(classes, scope, RenderContext::div, content)

        fun <CL : HTMLElement> RenderContext.radioGroupOptionLabel(
            classes: String? = null,
            scope: (ScopeContext.() -> Unit) = {},
            tag: TagFactory<Tag<CL>>,
            content: Tag<CL>.() -> Unit
        ) = tag(this, classes, "$optionId-label", scope, content).also { label = it }

        fun RenderContext.radioGroupOptionLabel(
            classes: String? = null,
            scope: (ScopeContext.() -> Unit) = {},
            content: Tag<HTMLLabelElement>.() -> Unit
        ) = radioGroupOptionLabel(classes, scope, RenderContext::label) {
            content()
            `for`(optionId)
        }

        fun <CL : HTMLElement> RenderContext.radioGroupOptionDescription(
            classes: String? = null,
            scope: (ScopeContext.() -> Unit) = {},
            tag: TagFactory<Tag<CL>>,
            content: Tag<CL>.() -> Unit
        ) = tag(
            this,
            classes,
            "$optionId-description-${descriptions.size}",
            scope,
            content
        ).also { descriptions.add(it) }

        fun RenderContext.radioGroupOptionDescription(
            classes: String? = null,
            scope: (ScopeContext.() -> Unit) = {},
            content: Tag<HTMLSpanElement>.() -> Unit
        ) = radioGroupOptionDescription(classes, scope, RenderContext::span, content)
    }

    fun <CO : HTMLElement> RenderContext.radioGroupOption(
        option: T,
        classes: String? = null,
        id: String? = null,
        scope: (ScopeContext.() -> Unit) = {},
        tag: TagFactory<Tag<CO>>,
        initialize: RadioGroupOption<CO>.() -> Unit
    ): Tag<CO> = tag(this, classes, id, scope) {
        RadioGroupOption(this, option, id).run {
            initialize()
            render()
        }
    }

    fun RenderContext.radioGroupOption(
        option: T,
        classes: String? = null,
        id: String? = null,
        scope: (ScopeContext.() -> Unit) = {},
        initialize: RadioGroupOption<HTMLDivElement>.() -> Unit
    ): Tag<HTMLDivElement> = radioGroupOption(option, classes, id, scope, RenderContext::div, initialize)
}

fun <C : HTMLElement, T> RenderContext.radioGroup(
    classes: String? = null,
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    tag: TagFactory<Tag<C>>,
    initialize: RadioGroup<C, T>.() -> Unit
): Tag<C> = tag(this, classes, id, scope) {
    RadioGroup<C, T>(this, id).run {
        initialize()
        render()
    }
}

fun <T> RenderContext.radioGroup(
    classes: String? = null,
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    initialize: RadioGroup<HTMLDivElement, T>.() -> Unit
): Tag<HTMLDivElement> = radioGroup(classes, id, scope, RenderContext::div, initialize)
