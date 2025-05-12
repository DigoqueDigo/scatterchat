#let todo = text.with(fill: red)

#let imageonside(lefttext, rightimage, bottomtext: none, marginleft: 0em, margintop: 0.5em) = {
  set par(justify: true)
  grid(columns: 2, column-gutter: 1em, lefttext, rightimage)
  set par(justify: false)
  block(inset: (left: marginleft, top: -margintop), bottomtext)
}

#let code_block(
  code: []
) = block(
    fill: luma(240),
    inset: 1pt,
    radius: 4pt,
    stroke: 1pt,
    width: 100%,
    code,
)

#let image_block(
  imagem: image,
  caption: []
) = figure(
    block(
      inset: 0.5pt,
      radius: 2pt,
      stroke: 1pt,
      imagem
    ),
    caption: caption,
)
