package dev.fritz2.headlessdemo

import dev.fritz2.core.*
import dev.fritz2.headless.components.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLTableRowElement

fun Tag<HTMLTableRowElement>.column(title: String, button: Tag<HTMLDivElement>.() -> Unit) {
    th("drop-shadow-sm pl-4 py-3 z-10 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 bg-gray-50") {
        div("w-full flex flex-row items-center") {
            p("flex-auto") {
                +title
            }
            div("flex-initial") {
                button()
            }
        }
    }
}

val sortIcons: DataCollection<Person, HTMLDivElement>.DataCollectionSortButton<HTMLButtonElement>.() -> Unit = {
    direction.render {
        icon(
            "text-gray-500 h-3 w-3 mt-1 mr-2", content =
            when (it) {
                SortDirection.NONE -> HeroIcons.selector
                SortDirection.ASC -> HeroIcons.sort_ascending
                SortDirection.DESC -> HeroIcons.sort_descending
            }
        )
    }
}

fun RenderContext.collectionDemo() {
    val examples = listOf(
        "DataTable" to RenderContext::dataTableDemo,
        "GridList" to RenderContext::gridListDemo,
    )

    tabGroup("w-full") {
        tabList("w-96 flex p-1 space-x-1 bg-blue-900/20 rounded-xl") {
            examples.forEach { (category, _) ->
                tab(
                    classes(
                        "w-full py-2.5 text-sm leading-5 font-medium text-blue-700 rounded-lg",
                        "focus:outline-none focus:ring-2 ring-offset-2 ring-offset-blue-400 ring-white ring-opacity-60"
                    )
                ) {
                    className(selected.map { sel ->
                        if (sel == index) "bg-white shadow"
                        else "text-blue-100 hover:bg-white/[0.12] hover:text-white"
                    })
                    +category
                }
            }
        }
        tabPanels("mt-2") {
            examples.forEach { (_, example) ->
                panel {
                    example(this)
                }
            }
        }
    }

}

fun RenderContext.dataTableDemo() {

//    val selectionStore = object : RootStore<Person?>(null) {}
    val persons = FakePersons(100)
    val storedPersons = storeOf(persons)
    val selectionStore = object : RootStore<List<Person>>(persons.take(2)) {}

    val filterStore = storeOf("")
    inputField("mt-2 mb-4") {
        value(filterStore)

        inputTextfield("shadow-sm focus:ring-indigo-500 focus:border-indigo-500 block w-1/2 sm:text-sm border-gray-300 px-4 rounded-full") {placeholder("filter...")
        }
    }

    dataCollection<Person>("shadow h-80 border border-gray-200 sm:rounded-lg overflow-y-auto overflow-x-auto relative") {
        data(storedPersons.data, Person::id)

//        selection.single(selectionStore)
        selection.multi(selectionStore)

        filterStore.data handledBy filterByText

        table("min-w-full divide-y divide-gray-200 bg-white") {
            thead {
                tr("divide-x divide-gray-100") {
                    column("Name") {
                        dataCollectionSortButton(
                            compareBy(Person::fullName),
                            compareByDescending(Person::fullName),
                            initialize = sortIcons
                        )
                    }
                    column("eMail") {
                        // TODO: Geht aktuell nicht, weil Sorting nicht public ist im Button
                        /*
                        dataCollectionSortButton(compareBy(Person::email), compareByDescending(Person::email)) {
                            keydowns.mapNotNull { if(shortcutOf(it) == Keys.Space) SORTING else null } handledBy sortBy
                        }

                         */
                    }
                    column("Birthday") {}
                }
            }

            val padding = "px-4 py-2 whitespace-nowrap"

            dataCollectionItems("text-sm font-medium text-gray-500 hover:bg-indigo-400", tag = RenderContext::tbody) {
                items.renderEach(Person::id) { item ->
                    dataCollectionItem(item, tag = RenderContext::tr) {
                        className(selected.combine(active) { sel, act ->
                            if (sel) {
                                if (act) "bg-indigo-200" else "bg-indigo-100"
                            } else {
                                if (act) "bg-indigo-50" else "odd:bg-white even:bg-gray-50"
                            }
                        })
                        td(padding) { +item.fullName }
                        td(padding) { +item.email }
                        td(padding) { +item.birthday }
                    }
                }
            }
        }

    }

    div("bg-gray-300 mt-4 p-2 rounded-lg ring-2 ring-gray-50") {
        em { +"Selected: " }
        ul("") {
            selectionStore.data.map { it.map { it.fullName } }.renderEach {
                li { +it }
            }
        }
    }
}

fun RenderContext.gridListDemo() {
    val persons = FakePersons(500)
    val storedPersons = storeOf(persons)
    val selectionStore = object : RootStore<List<Person>>(persons.take(2)) {}


    val filterStore = storeOf("")
    inputField("mt-2 mb-4") {
        value(filterStore)
        //FIXME: Warum braucht man den?
        placeholder("filter...")
        inputTextfield("shadow-sm focus:ring-indigo-500 focus:border-indigo-500 block w-1/2 sm:text-sm border-gray-300 px-4 rounded-full") { }
    }

    val sorting = Sorting(compareBy(Person::fullName), compareByDescending(Person::fullName))
    /*
    val storedSorting = storeOf(Sorting(compareBy(Person::fullName), compareByDescending(Person::fullName)))
    button {
        +"Sort"
        clicks.map { storedSorting.current } handledBy storedSorting.update
    }

     */

    dataCollection<Person>("shadow h-96 overflow-y-auto overflow-x-auto relative") {
        data(storedPersons.data, Person::id)

//        selection.single(selectionStore)
        selection.multi(selectionStore)

        filterStore.data handledBy filterByText

        // FIXME: Klassischer Observer
        // outerStore.updates handledBy sortBy

        button {
            +"Sort"
            // FIXME: Problem: Wenn in Map eine neue Instanz erzeugt wird, klappt das Sortieren nur einmal!
            clicks.map { sorting } handledBy sortBy
        }

        dataCollectionItems("grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 p-2", tag = RenderContext::ul) {
            attr("role", "list")
            items.renderEach(Person::id) { item ->
                dataCollectionItem(
                    item,
                    "col-span-1 bg-white rounded-lg shadow divide-y divide-gray-200 ring-offset-2 ring-offset-amber-400",
                    tag = RenderContext::li
                ) {
                    className(selected.combine(active) { sel, act ->
                        classes(
                            if (act) "ring-2 ring-white outline-none" else "ring-offset-2 ring-offset-amber-400",
                            if (sel) "bg-indigo-200" else "bg-white"
                        )
                    })
                    div("w-full flex items-center justify-between p-6 space-x-6") {
                        div("flex-1 truncate") {
                            div("flex items-center space-x-3") {
                                h3("text-gray-900 text-sm font-medium truncate") { +item.fullName }
                                span("flex-shrink-0 inline-block px-2 py-0.5 text-green-800 text-xs font-medium bg-green-100 rounded-full") { +item.birthday }
                            }
                            p("mt-1 text-gray-500 text-sm truncate") { +"${item.address.postalCode} ${item.address.city}, ${item.address.street} ${item.address.houseNumber}" }
                        }
                        img("w-10 h-10 bg-gray-300 rounded-full flex-shrink-0") {
                            src(item.portraitUrl)
                            alt("")
                        }
                    }
                    div {
                        div("-mt-px flex divide-x divide-gray-200") {
                            div("w-0 flex-1 flex") {
                                a("relative -mr-px w-0 flex-1 inline-flex items-center justify-center py-4 text-sm text-gray-700 font-medium border border-transparent rounded-bl-lg hover:text-gray-500") {
                                    href("mailto:janecooper@example.com")
                                    /* <!-- Heroicon name: solid/mail --> */
                                    svg("w-5 h-5 text-gray-400") {
                                        xmlns("http://www.w3.org/2000/svg")
                                        viewBox("0 0 20 20")
                                        fill("currentColor")
                                        attr("aria-hidden", "true")
                                        path {
                                            d("M2.003 5.884L10 9.882l7.997-3.998A2 2 0 0016 4H4a2 2 0 00-1.997 1.884z")
                                        }
                                        path {
                                            d("M18 8.118l-8 4-8-4V14a2 2 0 002 2h12a2 2 0 002-2V8.118z")
                                        }
                                    }
                                    span("ml-3 truncate") { +item.email }
                                }
                            }
                            div("-ml-px w-0 flex-1 flex") {
                                a("relative w-0 flex-1 inline-flex items-center justify-center py-4 text-sm text-gray-700 font-medium border border-transparent rounded-br-lg hover:text-gray-500") {
                                    href("tel:+1-202-555-0170")
                                    /* <!-- Heroicon name: solid/phone --> */
                                    svg("w-5 h-5 text-gray-400") {
                                        xmlns("http://www.w3.org/2000/svg")
                                        viewBox("0 0 20 20")
                                        fill("currentColor")
                                        attr("aria-hidden", "true")
                                        path {
                                            d("M2 3a1 1 0 011-1h2.153a1 1 0 01.986.836l.74 4.435a1 1 0 01-.54 1.06l-1.548.773a11.037 11.037 0 006.105 6.105l.774-1.548a1 1 0 011.059-.54l4.435.74a1 1 0 01.836.986V17a1 1 0 01-1 1h-2C7.82 18 2 12.18 2 5V3z")
                                        }
                                    }
                                    span("ml-3") { +item.phone }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    div("bg-gray-300 mt-4 p-2 rounded-lg ring-2 ring-gray-50") {
        em { +"Selected: " }
        ul("") {
            selectionStore.data.map { it.map { it.fullName } }.renderEach {
                li { +it }
            }
        }
    }
}