#let project(title: "", subtitle: "", authors: (), body) = {
  set document(title: title, author: authors.map(a => a.name))
  set page(numbering: "1", number-align: center)
  set text(font: "IBM Plex Sans", lang: "pt", region: "PT", size: 
  10pt)


  set par(justify: true)
  
  show figure: set block(breakable: true)
  show link: set text(fill: blue.darken(30%))
  show math.equation: set text(font: "Fira Math")
  show link: underline
  
  // Display inline code in a small box
  // that retains the correct baseline
  show raw.where(block: false): box.with(
    fill: luma(240),
    inset: (x: 3pt, y: 0pt),
    outset: (y: 3pt),
    radius: 2pt
  )
    
  // Display block code in a larger block
  // with more padding
  show raw.where(block: true): block.with(
    breakable: true,
    fill: luma(240),
    inset: 10pt,
    radius: 4pt
  )
  
  align(center)[
    #block(text(weight: 700, 1.75em, title))
    #block(text(weight: 500, 1.2em, subtitle))
  ]
  
  pad(top: 0.5em, bottom: 0.5em, x: 2em,
    grid(
      columns: (1fr,) * calc.min(4, authors.len()),
      gutter: 1em,
      ..authors.map(author => align(center)[
        *#author.name* \
        #author.affiliation
      ]),
    ),
  )

  body
}
