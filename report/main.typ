#import "template.typ": *

#show: project.with(
  title: [ScatterChat],
  subtitle: [Paradigmas de Sistemas Distribuídos \ Sistemas Distribuídos em Grande Escala],
  authors:(
    (name: "Diogo Marques", affiliation: "PG55931"),
    (name: "Francisco Ferreira", affiliation: "PG55942"),
    (name: "Ivan Ribeiro", affiliation: "PG55950"),
  )
)

#let TODO(..content) = {
  align(center)[
  #text("TODO!\n", size: 40pt, fill: red, weight:"extrabold")
  #text(size: 15pt,fill: red,weight: "bold",..content)
  ]
}

#include "chapters/intro.typ"
#include "chapters/dht.typ"
#include "chapters/aggregation.typ"
#include "chapters/chat.typ"
#include "chapters/client.typ"
#include "chapters/conclusion.typ"