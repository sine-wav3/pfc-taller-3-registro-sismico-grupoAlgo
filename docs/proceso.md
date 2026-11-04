# Informe de proceso: Taller 3, registro sísmico

Este informe describe, paso a paso, cómo se ejecuta cada función del taller, y en qué operaciones se traduce cada expresión `for` utilizada. Para cada función se presenta el código, su traducción, la explicación de cada componente, una traza completa con el registro de ejemplo, los casos borde y el costo.

## 0. Convenciones y datos de ejemplo

Los tipos del modelo de datos no se modifican:

```scala
case class Fecha(anio: Int, mes: Int)
case class Sismo(fecha: Fecha, region: String, magnitud: Double, profundidad: Int)
type Registro = List[Sismo]
```

En las trazas se numeran los sismos del registro de ejemplo del enunciado del 1 al 7:

| N° | Fecha | Región | Magnitud | Profundidad |
|----|-------|--------|----------|-------------|
| 1 | 2025-03 | Valle | 4.2 | 30 |
| 2 | 2025-03 | Santander | 5.1 | 150 |
| 3 | 2025-04 | Valle | 3.8 | 25 |
| 4 | 2025-04 | Choco | 6.0 | 40 |
| 5 | 2025-05 | Santander | 4.7 | 160 |
| 6 | 2025-05 | Valle | 5.3 | 35 |
| 7 | 2025-06 | Choco | 3.1 | 20 |

Restricciones de estilo que cumplen todas las funciones: no hay variables mutables (`var`), no hay ciclos iterativos (`while` ni `for` sin `yield`) y no hay `return`. Cada función es una única expresión cuyo valor es el resultado.

---

## 1. Cómo se traduce una expresión `for`

Una expresión `for ... yield` no es un ciclo. Es una notación que el compilador reescribe, antes de generar código, en llamadas a los métodos `withFilter`, `map` y `flatMap` de la colección. La reescritura sigue reglas fijas:

| Forma escrita con `for` | Traducción del compilador |
|---|---|
| `for { x <- xs } yield f(x)` | `xs.map(x => f(x))` |
| `for { x <- xs; if p(x) } yield f(x)` | `xs.withFilter(x => p(x)).map(x => f(x))` |
| `for { x <- xs; y <- ys } yield f(x, y)` | `xs.flatMap(x => ys.map(y => f(x, y)))` |
| `for { x <- xs; y <- ys; if p(x, y) } yield f(x, y)` | `xs.flatMap(x => ys.withFilter(y => p(x, y)).map(y => f(x, y)))` |
| Dos guardas seguidas `if p; if q` | Dos `withFilter` encadenados, que equivalen a una guarda `p && q` |

Reglas generales:

1. Cada generador (`x <- xs`) introduce una variable que recorre una colección.
2. El último generador, el que va antes del `yield`, se traduce a `map`.
3. Todos los generadores anteriores se traducen a `flatMap`. Cada elemento del generador exterior produce una lista, y `flatMap` concatena todas esas listas en el orden en que se producen.
4. Cada guarda (`if`) se traduce a `withFilter`, aplicado sobre el generador que la precede.
5. Cuando el patrón del generador es una tupla, como `(a, i) <- ...`, el compilador lo traduce a una función con `case` (`{ case (a, i) => ... }`). En Scala 2.13 puede añadir además un `withFilter` que comprueba que el elemento tenga la forma de tupla; para pares siempre se cumple, por lo que no cambia el resultado.

Un detalle sobre `withFilter`: a diferencia de `filter`, no construye una lista intermedia con los elementos que pasan. Devuelve un objeto que aplica la condición en el momento en que se ejecuta el `map` o `flatMap` siguiente. Por eso la traducción del `for` con guarda recorre la lista una sola vez.

```mermaid
flowchart TD
    A["Expresión for con yield"] --> B{"Cuántos generadores"}
    B -->|"Uno"| C["map sobre el generador"]
    B -->|"Varios"| D["flatMap para los generadores exteriores"]
    D --> E["map para el último generador"]
    A --> F{"Hay guardas if"}
    F -->|"Sí"| G["withFilter antes del map o flatMap"]
    F -->|"No"| H["Sin filtro"]
    C --> I["Nueva List"]
    E --> I
    G --> I
    H --> I
```

---

## 2. Punto 1: consultas con expresiones `for`

### 2.1. `fuertes(r, umbral)`

```scala
def fuertes(r: Registro, umbral: Double): List[String] = {
  for {
    s <- r
    if s.magnitud > umbral
  } yield s.region
}
```

**Traducción del `for`:**

```scala
r.withFilter(s => s.magnitud > umbral).map(s => s.region)
```

**Qué hace cada línea:**

1. `s <- r` recorre el registro de izquierda a derecha, en el orden en que están almacenados los sismos, y va asignando cada sismo a la variable `s`.
2. `if s.magnitud > umbral` es la guarda: solo continúan los sismos cuya magnitud es estrictamente mayor que el umbral. Un sismo con magnitud igual al umbral no pasa.
3. `yield s.region` produce, por cada sismo que pasó la guarda, el nombre de su región. La lista resultante tiene un elemento por cada sismo que pasó.

**Traza** de `fuertes(registro, 5.0)`:

| Sismo | Magnitud | Condición `magnitud > 5.0` | Elemento producido |
|---|---|---|---|
| 1 | 4.2 | falsa | ninguno |
| 2 | 5.1 | verdadera | `"Santander"` |
| 3 | 3.8 | falsa | ninguno |
| 4 | 6.0 | verdadera | `"Choco"` |
| 5 | 4.7 | falsa | ninguno |
| 6 | 5.3 | verdadera | `"Valle"` |
| 7 | 3.1 | falsa | ninguno |

Resultado: `List("Santander", "Choco", "Valle")`, igual al del enunciado.

**Propiedades y casos borde:**

- El orden del resultado coincide con el orden del registro, porque el generador recorre la lista en orden y `map` conserva ese orden.
- No se eliminan repetidos: si dos sismos fuertes son de la misma región, esa región aparece dos veces. Esto es lo que pide el enunciado y por eso no se usa `distinct`.
- Si el registro es vacío, el generador no produce ningún elemento y el resultado es `List()`.
- Si el umbral es mayor que todas las magnitudes, ninguna guarda se cumple y el resultado es `List()`.
- Si el umbral es menor que todas las magnitudes, el resultado contiene una región por cada sismo del registro.

**Costo:** una pasada sobre el registro, es decir, tiempo proporcional a $n$, con $n$ el número de sismos.

---

### 2.2. `enRegion(r, region)`

```scala
def enRegion(r: Registro, region: String): List[Double] = {
  for {
    s <- r
    if s.region == region
  } yield s.magnitud
}
```

**Traducción del `for`:**

```scala
r.withFilter(s => s.region == region).map(s => s.magnitud)
```

**Qué hace cada línea:**

1. `s <- r` recorre el registro en orden.
2. `if s.region == region` deja pasar solo los sismos de la región pedida. La comparación `==` entre `String` compara el contenido, no la referencia.
3. `yield s.magnitud` proyecta la magnitud de cada sismo que pasó.

Tiene la misma estructura que `fuertes`. Cambian dos cosas: la guarda compara regiones en lugar de magnitudes, y el `yield` entrega la magnitud en lugar de la región. Por eso el tipo de retorno es `List[Double]`.

**Traza** de `enRegion(registro, "Valle")`:

| Sismo | Región | Condición `region == "Valle"` | Elemento producido |
|---|---|---|---|
| 1 | Valle | verdadera | `4.2` |
| 2 | Santander | falsa | ninguno |
| 3 | Valle | verdadera | `3.8` |
| 4 | Choco | falsa | ninguno |
| 5 | Santander | falsa | ninguno |
| 6 | Valle | verdadera | `5.3` |
| 7 | Choco | falsa | ninguno |

Resultado: `List(4.2, 3.8, 5.3)`, en el orden de aparición en el registro.

**Casos borde:**

- Una región que no está en el registro, como `"Amazonas"`, no cumple la guarda en ningún sismo, y el resultado es `List()` sin lanzar excepción.
- La comparación distingue mayúsculas: `"valle"` no es igual a `"Valle"`.
- Registro vacío: resultado `List()`.

**Costo:** una pasada, proporcional a $n$.

---

### 2.3. `paresCercanos(r, dif)`

```scala
def paresCercanos(r: Registro, dif: Double): List[(String, String)] = {
  for {
    (a, i) <- r.map(_.region).distinct.zipWithIndex
    (b, j) <- r.map(_.region).distinct.zipWithIndex
    if i < j
    if r.exists(s1 => s1.region == a &&
      r.exists(s2 => s2.region == b &&
        Math.abs(s1.magnitud - s2.magnitud) <= dif))
  } yield (a, b)
}
```

**Traducción del `for`** (se escribe `regiones` para abreviar `r.map(_.region).distinct.zipWithIndex`):

```scala
regiones.flatMap { case (a, i) =>
  regiones
    .withFilter { case (b, j) => i < j }
    .withFilter { case (b, j) =>
      r.exists(s1 => s1.region == a &&
        r.exists(s2 => s2.region == b &&
          Math.abs(s1.magnitud - s2.magnitud) <= dif)) }
    .map { case (b, j) => (a, b) }
}
```

Como hay dos generadores, el primero se traduce a `flatMap` y el segundo a `map`. Las dos guardas se traducen a dos `withFilter` encadenados sobre el segundo generador.

**Construcción de la lista de regiones con índice**, paso a paso:

| Paso | Expresión | Valor con el registro de ejemplo |
|---|---|---|
| 1 | `r.map(_.region)` | `List(Valle, Santander, Valle, Choco, Santander, Valle, Choco)` |
| 2 | `.distinct` | `List(Valle, Santander, Choco)` |
| 3 | `.zipWithIndex` | `List((Valle,0), (Santander,1), (Choco,2))` |

`distinct` conserva la primera aparición de cada región, por lo que las regiones quedan en el orden en que aparecen por primera vez en el registro. `zipWithIndex` agrega a cada región su posición en esa lista, empezando en 0.

**Qué hace cada componente del `for`:**

1. El primer generador `(a, i) <- ...` toma cada región `a` con su índice `i`.
2. El segundo generador `(b, j) <- ...` toma, para cada `a`, todas las regiones `b` con su índice `j`. Esto forma el producto cartesiano: con 3 regiones se examinan 9 combinaciones.
3. La guarda `if i < j` conserva solo las combinaciones en las que `a` aparece antes que `b`. Con esto se descartan las combinaciones de una región consigo misma (`i == j`) y las parejas en orden inverso (`i > j`), de modo que cada pareja de regiones distintas se examina una sola vez.
4. La guarda `if r.exists(...)` comprueba si existe un sismo `s1` de la región `a` y un sismo `s2` de la región `b` cuyas magnitudes difieren en valor absoluto en menos o exactamente `dif`. El `exists` interior se evalúa para cada `s1` de la región `a`.
5. `yield (a, b)` construye la pareja de nombres de región.

**Magnitudes de cada región y diferencias entre regiones** (con el registro de ejemplo):

| Región | Magnitudes |
|---|---|
| Valle | 4.2, 3.8, 5.3 |
| Santander | 5.1, 4.7 |
| Choco | 6.0, 3.1 |

| Pareja | Diferencias entre todas las combinaciones | Menor diferencia |
|---|---|---|
| Valle y Santander | 0.9, 0.5, 1.3, 0.9, 0.2, 0.6 | 0.2 (5.3 contra 5.1) |
| Valle y Choco | 1.8, 1.1, 2.2, 0.7, 0.7, 2.2 | 0.7 (3.8 contra 3.1 y 5.3 contra 6.0) |
| Santander y Choco | 0.9, 2.0, 1.3, 1.6 | 0.9 (5.1 contra 6.0) |

**Traza** de `paresCercanos(registro, 0.6)`, con las nueve combinaciones del producto cartesiano:

| `(a, i)` | `(b, j)` | `i < j` | Menor diferencia | Menor o igual a 0.6 | Resultado |
|---|---|---|---|---|---|
| Valle, 0 | Valle, 0 | no | no se evalúa | no se evalúa | descartada |
| Valle, 0 | Santander, 1 | sí | 0.2 | sí | se produce `(Valle, Santander)` |
| Valle, 0 | Choco, 2 | sí | 0.7 | no | descartada |
| Santander, 1 | Valle, 0 | no | no se evalúa | no se evalúa | descartada |
| Santander, 1 | Santander, 1 | no | no se evalúa | no se evalúa | descartada |
| Santander, 1 | Choco, 2 | sí | 0.9 | no | descartada |
| Choco, 2 | Valle, 0 | no | no se evalúa | no se evalúa | descartada |
| Choco, 2 | Santander, 1 | no | no se evalúa | no se evalúa | descartada |
| Choco, 2 | Choco, 2 | no | no se evalúa | no se evalúa | descartada |

Resultado: `List(("Valle", "Santander"))`.

**Otros umbrales con el mismo registro:**

- Con `dif = 1.0`, las tres parejas cumplen (0.2, 0.7 y 0.9 son menores o iguales a 1.0). El `flatMap` las entrega en el orden `(Valle, Santander)`, `(Valle, Choco)`, `(Santander, Choco)`.
- Con `dif = 0.1`, ninguna pareja cumple (la menor diferencia entre regiones distintas es 0.2). El resultado es `List()`.

**Por qué el orden del resultado es el pedido:** el `flatMap` recorre primero las regiones `a` en orden de primera aparición y, para cada una, el `map` interior recorre las regiones `b` en ese mismo orden. Por tanto las parejas salen ordenadas por la posición de `a` y, a igual `a`, por la posición de `b`. Como además `i < j`, la pareja siempre queda orientada con la región que apareció primero a la izquierda: sale `("Valle", "Choco")` y nunca `("Choco", "Valle")`.

**Casos borde:**

- Registro vacío: `distinct` produce `List()`, no hay combinaciones y el resultado es `List()`.
- Registro con una sola región: solo existe la combinación `(0, 0)`, que no cumple `i < j`, y el resultado es `List()`.
- `dif = 0.0`: solo se produce una pareja si dos regiones distintas tienen una magnitud exactamente igual.
- Las comparaciones se hacen con `Double`. Si la diferencia real de dos magnitudes es igual a `dif`, el error de representación en punto flotante puede inclinar la comparación hacia un lado u otro, por lo que conviene no apoyarse en igualdades exactas en la frontera.

**Costo:** sea $k$ el número de regiones distintas. La lista `regiones` se reconstruye en cada iteración del generador exterior, con un costo de $O(n)$ por reconstrucción. Se examinan $O(k^2)$ combinaciones y, para cada una que pasa `i < j`, el `exists` anidado puede costar hasta $O(n^2)$ en el peor caso. El costo total es del orden de $O(k^2 \cdot n^2)$. Para el tamaño del registro del taller es aceptable, y el objetivo del punto es que el código describa qué se busca.

---

## 3. Punto 2: agrupar con mapas

En este punto no se usan expresiones `for`. Se usan las operaciones de agrupamiento de la librería estándar de Scala, que devuelven un `Map` directamente. Así se evita recorrer listas a mano y mantener acumuladores mutables.

### 3.1. `porRegion(r)`

```scala
def porRegion(r: Registro): Map[String, List[Sismo]] = {
  r.groupBy(_.region)
}
```

`groupBy(f)` aplica la función `f` a cada elemento para obtener su llave, y devuelve un `Map` de cada llave a la lista de los elementos que tienen esa llave. Dentro de cada lista se conserva el orden relativo que tenían los elementos en el registro original. La función `f` aquí es `_.region`, es decir, la región del sismo.

**Traza**, con el estado del mapa después de procesar cada sismo:

| Sismo procesado | Llave | Estado del mapa |
|---|---|---|
| 1 | Valle | Valle: [1] |
| 2 | Santander | Valle: [1]; Santander: [2] |
| 3 | Valle | Valle: [1, 3]; Santander: [2] |
| 4 | Choco | Valle: [1, 3]; Santander: [2]; Choco: [4] |
| 5 | Santander | Valle: [1, 3]; Santander: [2, 5]; Choco: [4] |
| 6 | Valle | Valle: [1, 3, 6]; Santander: [2, 5]; Choco: [4] |
| 7 | Choco | Valle: [1, 3, 6]; Santander: [2, 5]; Choco: [4, 7] |

Resultado: `Map("Valle" -> List(sismos 1, 3, 6), "Santander" -> List(sismos 2, 5), "Choco" -> List(sismos 4, 7))`.

**Casos borde:**

- Una llave solo se crea cuando aparece un elemento con ella, por lo que las regiones que no están en el registro no aparecen en el mapa. Pedir una de ellas con `apply` lanza excepción, y de eso se ocupa el Punto 3.
- Registro vacío: resultado `Map()`.
- Toda lista del mapa tiene al menos un elemento.

**Costo:** una pasada sobre el registro, proporcional a $n$, más el costo de las operaciones sobre el mapa por cada elemento.

---

### 3.2. `conteoPorRegion(r)`

```scala
def conteoPorRegion(r: Registro): Map[String, Int] = {
  r.groupMapReduce(_.region)(_ => 1)(_ + _)
}
```

`groupMapReduce(llave)(valor)(combinar)` recibe tres funciones, en tres listas de parámetros:

1. `_.region`: determina a qué grupo pertenece cada sismo.
2. `_ => 1`: determina qué valor aporta cada sismo a su grupo. Cada sismo aporta siempre 1.
3. `_ + _`: determina cómo se combinan dos valores del mismo grupo. Aquí se suman.

Es equivalente a agrupar primero, transformar después y reducir al final:

```scala
r.groupBy(_.region).view.mapValues(grupo => grupo.map(_ => 1).reduce(_ + _)).toMap
```

La diferencia es que `groupMapReduce` lo hace en una sola pasada y no construye las listas intermedias de sismos por región ni las listas de unos. Esa es la razón por la que se prefirió a `groupBy` seguido de `size`.

**Traza**, con el acumulado de cada región después de procesar cada sismo:

| Sismo procesado | Región | Valor aportado | Acumulado de esa región |
|---|---|---|---|
| 1 | Valle | 1 | Valle = 1 |
| 2 | Santander | 1 | Santander = 1 |
| 3 | Valle | 1 | Valle = 1 + 1 = 2 |
| 4 | Choco | 1 | Choco = 1 |
| 5 | Santander | 1 | Santander = 1 + 1 = 2 |
| 6 | Valle | 1 | Valle = 2 + 1 = 3 |
| 7 | Choco | 1 | Choco = 1 + 1 = 2 |

Resultado: `Map("Valle" -> 3, "Santander" -> 2, "Choco" -> 2)`.

**Casos borde:**

- Registro vacío: no hay grupos y el resultado es `Map()`.
- Una región con un único sismo tiene conteo 1; en ese caso la función de combinación nunca se aplica a ese grupo, pues basta con el primer valor.
- Los conteos suman siempre $n$, el tamaño del registro.

**Costo:** una pasada, proporcional a $n$.

---

### 3.3. `magnitudMaxima(r)`

```scala
def magnitudMaxima(r: Registro): Map[String, Double] = {
  r.groupMapReduce(_.region)(_.magnitud)(math.max)
}
```

Tiene la misma estructura que `conteoPorRegion`, cambiando el valor que aporta cada sismo y la forma de combinar:

1. `_.region`: el grupo es la región.
2. `_.magnitud`: cada sismo aporta su magnitud.
3. `math.max`: al combinar dos magnitudes del mismo grupo se conserva la mayor. Se pasa la función `math.max` directamente, porque recibe dos `Double` y devuelve un `Double`, que es la forma que espera el tercer parámetro.

**Traza**, con el máximo acumulado de cada región:

| Sismo procesado | Región | Magnitud aportada | Máximo acumulado de esa región |
|---|---|---|---|
| 1 | Valle | 4.2 | Valle = 4.2 |
| 2 | Santander | 5.1 | Santander = 5.1 |
| 3 | Valle | 3.8 | Valle = max(4.2, 3.8) = 4.2 |
| 4 | Choco | 6.0 | Choco = 6.0 |
| 5 | Santander | 4.7 | Santander = max(5.1, 4.7) = 5.1 |
| 6 | Valle | 5.3 | Valle = max(4.2, 5.3) = 5.3 |
| 7 | Choco | 3.1 | Choco = max(6.0, 3.1) = 6.0 |

Resultado: `Map("Valle" -> 5.3, "Santander" -> 5.1, "Choco" -> 6.0)`.

**Casos borde:**

- Cada grupo existe solo si tiene al menos un sismo, así que la reducción nunca se aplica a una colección vacía. Por eso no se necesita `reduceOption` ni se lanza excepción.
- Registro vacío: resultado `Map()`.
- `math.max` devuelve uno de sus dos argumentos sin modificarlo, por lo que el valor entregado es exactamente la mayor magnitud registrada, sin error de redondeo adicional.

**Costo:** una pasada, proporcional a $n$.

```mermaid
flowchart TD
    A["Registro: List de Sismo"] --> B["Para cada sismo calcular la llave: su región"]
    B --> C["Para cada sismo calcular el valor: 1 o su magnitud"]
    C --> D["Si la llave ya existe: combinar con el acumulado usando suma o max"]
    C --> E["Si la llave no existe: crear la entrada con el valor"]
    D --> F["Map de región a resultado"]
    E --> F
```

Las tres consultas del Punto 1 se resuelven con expresiones `for`, como exige el enunciado, y los tres resúmenes del Punto 2 se resuelven con mapas y no con listas recorridas a mano.

## Punto 3: consultas seguras

## Convenciones y datos de ejemplo

Los tipos del modelo de datos no se modifican:

```scala
case class Fecha(anio: Int, mes: Int)
case class Sismo(fecha: Fecha, region: String, magnitud: Double, profundidad: Int)
type Registro = List[Sismo]
```

En las trazas se numeran los sismos del registro de ejemplo del enunciado del 1 al 7:

| N° | Fecha | Región | Magnitud | Profundidad |
|----|-------|--------|----------|-------------|
| 1 | 2025-03 | Valle | 4.2 | 30 |
| 2 | 2025-03 | Santander | 5.1 | 150 |
| 3 | 2025-04 | Valle | 3.8 | 25 |
| 4 | 2025-04 | Choco | 6.0 | 40 |
| 5 | 2025-05 | Santander | 4.7 | 160 |
| 6 | 2025-05 | Valle | 5.3 | 35 |
| 7 | 2025-06 | Choco | 3.1 | 20 |

Restricciones de estilo que cumplen todas las funciones: no hay variables mutables (`var`), no hay ciclos iterativos (`while` ni `for` sin `yield`) y no hay `return`. Las funciones del Punto 3 son una única expresión cuyo valor es el resultado. La del Punto 5 es un bloque cuyas definiciones locales son todas `val` inmutables y cuyo último valor es el resultado.

En ninguno de los dos puntos se usan expresiones `for`. Se usan las operaciones de agrupamiento, filtrado, proyección y ordenamiento de las colecciones de Scala, que se explican en cada punto.

---

### Operaciones de colección usadas en el Punto 3

| Operación | Qué hace | Dónde se usa |
|---|---|---|
| `groupBy(f)` | Devuelve un `Map` de cada llave `f(x)` a la lista de los elementos con esa llave, conservando el orden relativo | `conteoSeguro`, `regionMasActiva` |
| `map` sobre un `Map` con función de pares | Transforma cada entrada `(llave, valor)` en un nuevo par y construye un `Map` con ellos | `conteoSeguro` |
| `withDefaultValue(d)` | Envuelve el mapa para que `apply` devuelva `d` cuando la llave no está, sin agregar la llave | `conteoSeguro` |
| `filter(p)` | Lista de los elementos que cumplen `p`, en el mismo orden | `maximaDe` |
| `map(f)` sobre una lista | Lista de las imágenes por `f`, en el mismo orden | `maximaDe` |
| `maxOption` | `Some(máximo)` si la lista no está vacía y `None` si lo está | `maximaDe` |
| `toList` sobre un `Map` | Lista de pares `(llave, valor)`, uno por entrada | `regionMasActiva` |
| `sortBy(f)` | Ordena la lista por la llave `f(x)`; las tuplas se comparan componente a componente | `regionMasActiva` |
| `headOption` | `Some(primer elemento)` o `None` si la lista es vacía | `regionMasActiva` |
| `Option.map(f)` | Aplica `f` al contenido de un `Some` y deja `None` como está | `regionMasActiva` |

---

### `conteoSeguro(r)`

```scala
def conteoSeguro(r: Registro): Map[String, Int] = {
  r.groupBy(_.region).map { case (region, sismos) =>
    region -> sismos.size
  }.withDefaultValue(0)
}
```

**Qué hace cada componente:**

1. `r.groupBy(_.region)` agrupa los sismos por región. El resultado es un `Map[String, List[Sismo]]` con una entrada por cada región presente en el registro.
2. `.map { case (region, sismos) => region -> sismos.size }` recorre las entradas del mapa. El patrón `case (region, sismos)` descompone cada par en su llave y su lista. Por cada entrada produce un par nuevo con la misma región y el tamaño de la lista. El resultado es un `Map[String, Int]`.
3. `.withDefaultValue(0)` devuelve el mismo mapa con un comportamiento distinto en `apply`: si la llave no existe, devuelve `0` en lugar de lanzar `NoSuchElementException`.

**Traza**, paso por paso con el registro de ejemplo:

| Paso | Expresión | Valor |
|---|---|---|
| 1 | `r.groupBy(_.region)` | `Map(Valle -> List(1, 3, 6), Santander -> List(2, 5), Choco -> List(4, 7))` |
| 2 | Entrada `(Valle, List(1, 3, 6))` | produce `Valle -> 3` |
| 3 | Entrada `(Santander, List(2, 5))` | produce `Santander -> 2` |
| 4 | Entrada `(Choco, List(4, 7))` | produce `Choco -> 2` |
| 5 | Mapa construido por el `map` | `Map(Valle -> 3, Santander -> 2, Choco -> 2)` |
| 6 | `.withDefaultValue(0)` | el mismo mapa, con valor por defecto `0` |

**Consultas sobre el resultado:**

| Consulta | Resultado | Por qué |
|---|---|---|
| `m("Valle")` | `3` | la llave existe |
| `m("Choco")` | `2` | la llave existe |
| `m("Amazonas")` | `0` | la llave no existe, responde el valor por defecto |
| `m.contains("Amazonas")` | `false` | el valor por defecto no agrega llaves |
| `m.get("Amazonas")` | `None` | igual que el anterior |

**Casos borde:**

- Registro vacío: `groupBy` produce `Map()`, el `map` produce `Map()` y toda consulta devuelve `0`.
- Región con un único sismo: el conteo es `1`.
- El valor por defecto no modifica el contenido del mapa. Solo cambia lo que ocurre al pedir una llave ausente con `apply`. Por eso `contains` y `get` siguen informando la ausencia.
- Para toda región presente, el valor coincide con el de `conteoPorRegion` del Punto 2. La diferencia es que `conteoSeguro` está definido para cualquier región.

**Costo:** `groupBy` recorre el registro una vez, con costo proporcional a $n$. El `map` recorre las $k$ entradas del mapa, y cada `size` de una lista cuesta lo que mide esa lista, por lo que en total suma $n$. El costo total es $O(n)$. A diferencia de `groupMapReduce`, este enfoque construye las listas de sismos por región antes de contarlas.

---

### `maximaDe(r, region)`

```scala
def maximaDe(r: Registro, region: String): Option[Double] = {
  r.filter(_.region == region).map(_.magnitud).maxOption
}
```

**Qué hace cada componente:**

1. `r.filter(_.region == region)` deja solo los sismos de la región pedida, en el orden del registro. La comparación `==` entre `String` compara el contenido.
2. `.map(_.magnitud)` convierte esa lista de sismos en la lista de sus magnitudes.
3. `.maxOption` calcula el máximo de esa lista si tiene elementos, y devuelve `None` si está vacía. La diferencia con `max` es justamente esa: `max` lanza excepción sobre una lista vacía.

**Traza** de `maximaDe(registro, "Valle")`:

| Paso | Expresión | Valor |
|---|---|---|
| 1 | `filter(_.region == "Valle")` | `List(sismo 1, sismo 3, sismo 6)` |
| 2 | `map(_.magnitud)` | `List(4.2, 3.8, 5.3)` |
| 3 | `maxOption`, acumulado tras 4.2 | `4.2` |
| 4 | `maxOption`, acumulado tras 3.8 | `max(4.2, 3.8) = 4.2` |
| 5 | `maxOption`, acumulado tras 5.3 | `max(4.2, 5.3) = 5.3` |
| 6 | Resultado | `Some(5.3)` |

**Otras consultas con el mismo registro:**

| Consulta | Lista de magnitudes | Resultado |
|---|---|---|
| `maximaDe(registro, "Santander")` | `List(5.1, 4.7)` | `Some(5.1)` |
| `maximaDe(registro, "Choco")` | `List(6.0, 3.1)` | `Some(6.0)` |
| `maximaDe(registro, "Amazonas")` | `List()` | `None` |

**Casos borde:**

- Región ausente: el filtro no deja ningún sismo, la lista de magnitudes es vacía y el resultado es `None`, sin excepción.
- Registro vacío: el resultado es `None` para cualquier región.
- La comparación distingue mayúsculas: `"valle"` produce `None` porque no es igual a `"Valle"`.
- El valor dentro del `Some` es exactamente una de las magnitudes del registro, porque el máximo de dos números devuelve uno de ellos sin modificarlo.
- Para toda región presente, el resultado es `Some` del valor que `magnitudMaxima` del Punto 2 tiene para esa región.

**Costo:** `filter` es una pasada sobre el registro, $O(n)$. `map` y `maxOption` recorren la lista filtrada, de tamaño a lo sumo $n$. El costo total es $O(n)$ por consulta. Cada consulta recorre el registro completo; si se hicieran muchas consultas, convendría construir antes `magnitudMaxima` y consultarlo.

---

### `regionMasActiva(r)`

```scala
def regionMasActiva(r: Registro): Option[String] = {
  r.groupBy(_.region)
    .toList
    .sortBy { case (region, sismos) => (-sismos.size, region) }
    .headOption
    .map(_._1)
}
```

**Qué hace cada componente:**

1. `r.groupBy(_.region)` agrupa los sismos por región, como en `conteoSeguro`.
2. `.toList` convierte el mapa en una lista de pares `(región, lista de sismos)`, con un par por región. El orden de esa lista depende de la iteración interna del mapa y no se debe usar como criterio.
3. `.sortBy { case (region, sismos) => (-sismos.size, region) }` ordena esa lista usando como llave la tupla `(-cantidad, región)`. Las tuplas se comparan primero por la primera componente y, si hay empate, por la segunda. Se usa la cantidad con signo negativo para que el orden creciente ponga primero la región con más sismos. En un empate de cantidad decide el nombre de la región en orden alfabético natural de las cadenas.
4. `.headOption` toma el primer par de la lista ordenada, o `None` si la lista es vacía.
5. `.map(_._1)` extrae el primer componente del par, la región, solo si el `Option` es `Some`. Si es `None`, lo deja como está.

**Traza** con el registro de ejemplo:

| Paso | Expresión | Valor |
|---|---|---|
| 1 | `groupBy(_.region)` | `Map(Valle -> List(1, 3, 6), Santander -> List(2, 5), Choco -> List(4, 7))` |
| 2 | `toList` | `List((Valle, [1,3,6]), (Santander, [2,5]), (Choco, [4,7]))` |
| 3 | Llaves de ordenamiento | `(-3, Valle)`, `(-2, Santander)`, `(-2, Choco)` |
| 4 | `sortBy` | `List((Valle, ...), (Choco, ...), (Santander, ...))` |
| 5 | `headOption` | `Some((Valle, [1,3,6]))` |
| 6 | `map(_._1)` | `Some("Valle")` |

En el paso 4, `(-3, Valle)` queda primero porque $-3 < -2$. Entre `(-2, Choco)` y `(-2, Santander)` la primera componente empata, y `"Choco"` va antes que `"Santander"` en orden alfabético.

**Ejemplo con empate**, para un registro con solo los sismos 2, 4, 5 y 7 (Santander y Choco con dos sismos cada una):

| Paso | Valor |
|---|---|
| Llaves | `(-2, Santander)`, `(-2, Choco)` |
| Lista ordenada | `Choco`, `Santander` |
| Resultado | `Some("Choco")` |

El desempate alfabético es lo que hace único el resultado. Sin él, la respuesta dependería del orden interno del mapa.

```mermaid
flowchart TD
    A["Registro: List de Sismo"] --> B["groupBy región: Map de región a sus sismos"]
    B --> C["toList: lista de pares región y sismos"]
    C --> D["sortBy con llave menos cantidad y región"]
    D --> E{"La lista está vacía"}
    E -->|"Sí"| F["headOption es None, resultado None"]
    E -->|"No"| G["headOption es el primer par"]
    G --> H["map extrae la región: Some de la región"]
```

**Casos borde:**

- Registro vacío: `groupBy` produce `Map()`, la lista es vacía, `headOption` devuelve `None` y el resultado es `None`.
- Una sola región: la lista tiene un único par y el resultado es `Some` de esa región.
- Todas las regiones con el mismo conteo: gana la primera en orden alfabético.
- El orden de `String` en Scala es por unidad de código: las mayúsculas van antes que las minúsculas y las letras con tilde después de las letras sin tilde. Con nombres escritos de forma homogénea, como los del registro, coincide con el orden alfabético esperado.
- Negar un conteo no desborda, porque todo conteo es al menos 1 y a lo sumo $n$.

**Costo:** `groupBy` es $O(n)$. Si el registro tiene $k$ regiones distintas, `toList` es $O(k)$ y `sortBy` es $O(k \log k)$ comparaciones de tuplas. El costo total es $O(n + k \log k)$, y como $k \le n$, queda acotado por $O(n \log n)$. Para obtener solo el primero bastaría una búsqueda del mínimo sin ordenar todo, pero ordenar deja el criterio de desempate explícito en la llave.

---

## Punto 4: réplicas perezosas

```scala
def replicas(magnitud: Double): LazyList[Double] =
  LazyList.iterate(redondear(magnitud))(x => redondear(x * 0.8))

def replicasSensibles(magnitud: Double, minimo: Double): List[Double] =
  replicas(magnitud).takeWhile(x => x > minimo).toList
```

Este punto no usa expresiones `for`, solo `LazyList.iterate` y `takeWhile`.

### Traza de `replicas(5.0).take(4).toList`

`LazyList.iterate(a)(f)` guarda la cabeza `a` y deja la cola como un cálculo
pendiente, `iterate(f(a))(f)`, que solo se ejecuta cuando alguien la pide.

| Paso | Qué se evalúa | Elementos calculados |
|---|---|---|
| 1 | `replicas(5.0)`: cabeza $5.0$, cola pendiente | $5.0$ |
| 2 | `take(4)`: no calcula nada, limita a 4 | $5.0$ |
| 3 | `toList` pide la cola: $\text{redondear}(5.0 \cdot 0.8)$ | $5.0,\ 4.0$ |
| 4 | pide la cola: $\text{redondear}(4.0 \cdot 0.8)$ | $5.0,\ 4.0,\ 3.2$ |
| 5 | pide la cola: $\text{redondear}(3.2 \cdot 0.8)$ | $5.0,\ 4.0,\ 3.2,\ 2.56$ |
| 6 | ya hay 4 elementos: `take` corta y no pide la cola | — |

```mermaid
sequenceDiagram
    participant T as toList
    participant K as take(4)
    participant L as LazyList.iterate
    T->>K: dame el siguiente
    K->>L: cabeza 5.0
    L-->>T: 5.0
    T->>L: cola pendiente
    L-->>T: 4.0
    T->>L: cola pendiente
    L-->>T: 3.2
    T->>L: cola pendiente
    L-->>T: 2.56
    K-->>T: fin (no se pide el quinto)
```

### Por qué no agota la memoria

La secuencia es infinita solo como definición. En memoria existen únicamente
los elementos que ya se pidieron; el resto es una función pendiente. Con
`take(4)` se calculan 4 elementos. Una `List` infinita no podría construirse,
porque exigiría calcular todos los elementos antes de devolver nada.

### Traza de `replicasSensibles(5.0, 3.0)`

`takeWhile(_ > 3.0)` evalúa elemento por elemento:
$5.0 > 3.0$ sí, $4.0 > 3.0$ sí, $3.2 > 3.0$ sí, $2.56 > 3.0$ no, y se detiene.
Resultado: `List(5.0, 4.0, 3.2)`. El 2.56 se calcula pero no entra, y no se
calcula nada más allá.

## Punto 5: informe combinado

El objetivo del punto es entregar, por cada región, un resumen con tres datos: cuántos sismos tuvo, cuál fue su magnitud máxima y cuántas réplicas se esperan a partir de ese sismo más fuerte. Combina el agrupamiento por región con la secuencia perezosa de réplicas del Punto 4.

### Operaciones y funciones usadas en el Punto 5

| Operación | Qué hace | Dónde se usa |
|---|---|---|
| `groupBy(f)` | Devuelve un `Map` de cada llave `f(x)` a la lista de los elementos con esa llave | primer paso de `informe` |
| `map` sobre un `Map` con función de pares | Transforma cada entrada `(llave, valor)` en un nuevo par y construye un `Map` con ellos | segundo paso de `informe` |
| `size` | Cantidad de elementos de una lista | conteo y cantidad de réplicas |
| `map(_.magnitud)` y `max` | Lista de magnitudes y su máximo; `max` lanza excepción si la lista es vacía | magnitud máxima |
| `replicasSensibles(m, minimo)` | Lista de réplicas esperadas de magnitud mayor que `minimo` (Punto 4) | cantidad de réplicas |
| `Resumen(conteo, maxima, replicas)` | Registro con los tres datos de una región | valor de cada entrada |

La función `replicasSensibles` pertenece al Punto 4 y se reproduce aquí para que este punto se entienda por sí solo:

```scala
def replicas(magnitud: Double): LazyList[Double] =
  LazyList.iterate(redondear(magnitud))(x => redondear(x * 0.8))

def replicasSensibles(magnitud: Double, minimo: Double): List[Double] =
  replicas(magnitud).takeWhile(x => x > minimo).toList
```

La función `redondear` redondea a dos decimales: $\mathrm{redondear}(x) = \mathrm{round}(100x)/100$. `replicas` es una secuencia perezosa infinita en la que cada elemento es el $80\,\%$ del anterior, redondeado. `takeWhile` toma elementos mientras sean mayores que `minimo` y se detiene en el primero que no lo sea, por lo que el resultado es finito.

---

### `informe(r, minimo)`

```scala
def informe(r: Registro, minimo: Double): Map[String, Resumen] = {
  r.groupBy(_.region).map { case (region, sismos) =>
    val conteo = sismos.size
    val maxima = sismos.map(_.magnitud).max
    val replicas = replicasSensibles(maxima, minimo).size

    region -> Resumen(conteo, maxima, replicas)
  }
}
```

**Qué hace cada componente:**

1. `r.groupBy(_.region)` agrupa los sismos por región y entrega un `Map[String, List[Sismo]]`. Cada región presente tiene una lista no vacía.
2. `.map { case (region, sismos) => ... }` recorre las entradas del mapa. Para cada región, el cuerpo del `case` calcula un `Resumen` y devuelve el par `region -> Resumen(...)`. Como la llave del par es la misma, el resultado es un `Map[String, Resumen]` con las mismas regiones.
3. `val conteo = sismos.size` es la cantidad de sismos de la región.
4. `val maxima = sismos.map(_.magnitud).max` extrae las magnitudes de los sismos de la región y toma la mayor. Es seguro usar `max` porque ninguna lista de `groupBy` es vacía.
5. `val replicas = replicasSensibles(maxima, minimo).size` calcula la lista de réplicas esperadas a partir de la magnitud máxima y toma su tamaño. Esa es la cantidad de réplicas esperadas.
6. `region -> Resumen(conteo, maxima, replicas)` junta los tres datos en el valor de la entrada.

Los `val` son definiciones inmutables que se evalúan una vez por región, en el orden en que están escritas. No son variables mutables.

**Traza** de `informe(registro, 3.0)`, región por región:

| Región | Sismos | `conteo` | Magnitudes | `maxima` |
|---|---|---|---|---|
| Valle | 1, 3, 6 | 3 | 4.2, 3.8, 5.3 | 5.3 |
| Santander | 2, 5 | 2 | 5.1, 4.7 | 5.1 |
| Choco | 4, 7 | 2 | 6.0, 3.1 | 6.0 |

Para obtener `replicas` se genera la secuencia a partir de `maxima`, redondeando a dos decimales en cada paso, y `takeWhile(_ > 3.0)` conserva los elementos hasta el primero que ya no supera `3.0`:

| Región | Secuencia de réplicas | Elementos mayores que 3.0 | Se detiene en | `replicas` |
|---|---|---|---|---|
| Valle | 5.3, 4.24, 3.39, 2.71 | 5.3, 4.24, 3.39 | 2.71 | 3 |
| Santander | 5.1, 4.08, 3.26, 2.61 | 5.1, 4.08, 3.26 | 2.61 | 3 |
| Choco | 6.0, 4.8, 3.84, 3.07, 2.46 | 6.0, 4.8, 3.84, 3.07 | 2.46 | 4 |

El detalle de los cálculos de cada secuencia es:

| Región | Paso | Cálculo | Valor redondeado |
|---|---|---|---|
| Valle | 1 | `redondear(5.3)` | 5.3 |
| Valle | 2 | `redondear(5.3 * 0.8) = redondear(4.24)` | 4.24 |
| Valle | 3 | `redondear(4.24 * 0.8) = redondear(3.392)` | 3.39 |
| Valle | 4 | `redondear(3.39 * 0.8) = redondear(2.712)` | 2.71 |
| Santander | 1 | `redondear(5.1)` | 5.1 |
| Santander | 2 | `redondear(5.1 * 0.8) = redondear(4.08)` | 4.08 |
| Santander | 3 | `redondear(4.08 * 0.8) = redondear(3.264)` | 3.26 |
| Santander | 4 | `redondear(3.26 * 0.8) = redondear(2.608)` | 2.61 |
| Choco | 1 | `redondear(6.0)` | 6.0 |
| Choco | 2 | `redondear(6.0 * 0.8) = redondear(4.8)` | 4.8 |
| Choco | 3 | `redondear(4.8 * 0.8) = redondear(3.84)` | 3.84 |
| Choco | 4 | `redondear(3.84 * 0.8) = redondear(3.072)` | 3.07 |
| Choco | 5 | `redondear(3.07 * 0.8) = redondear(2.456)` | 2.46 |

Como la secuencia es perezosa, el elemento que hace parar a `takeWhile` se calcula, pero no se calcula ninguno posterior.

Resultado:

```scala
Map(
  "Valle"     -> Resumen(3, 5.3, 3),
  "Santander" -> Resumen(2, 5.1, 3),
  "Choco"     -> Resumen(2, 6.0, 4)
)
```

El valor de Choco, `Resumen(2, 6.0, 4)`, coincide con el del enunciado.

```mermaid
flowchart TD
    A["Registro: List de Sismo"] --> B["groupBy región: Map de región a sus sismos"]
    B --> C["map sobre cada entrada: región y lista de sismos"]
    C --> D["conteo: tamaño de la lista"]
    C --> E["maxima: máximo de las magnitudes"]
    E --> F["replicasSensibles de maxima y minimo"]
    F --> G["replicas: tamaño de esa lista"]
    D --> H["Resumen de conteo, maxima y replicas"]
    E --> H
    G --> H
    H --> I["Map de región a Resumen"]
```

**Casos borde:**

- Registro vacío: `groupBy` produce `Map()`, no se ejecuta ningún cuerpo del `case` y el resultado es `Map()`.
- `minimo` mayor o igual que la magnitud máxima de una región: el primer elemento de la secuencia ya no supera `minimo`, `takeWhile` no toma nada y `replicas` vale `0`. Por ejemplo, con `minimo = 10.0` el resultado es `Map("Valle" -> Resumen(3, 5.3, 0), "Santander" -> Resumen(2, 5.1, 0), "Choco" -> Resumen(2, 6.0, 0))`.
- Región con un único sismo: `conteo` es `1` y `maxima` es la magnitud de ese sismo.
- Precondición sobre `minimo`: el redondeo a dos decimales hace que la secuencia se estanque en `0.02`, pues $\mathrm{redondear}(0.02 \cdot 0.8) = \mathrm{redondear}(0.016) = 0.02$. Si `minimo` es menor que `0.02`, esos elementos siempre son mayores que `minimo`, `takeWhile` nunca se detiene y la evaluación no termina. Por eso `informe` se debe llamar con `minimo` mayor o igual que `0.02`.
- Para toda región, `conteo` coincide con el valor de `conteoPorRegion` (Punto 2), `maxima` coincide con el de `magnitudMaxima` (Punto 2) y `replicas` depende solo de `maxima` y `minimo` (Punto 4).
- El orden en que el `Map` presenta las regiones no forma parte del resultado: dos mapas con las mismas entradas son iguales.

**Costo:** `groupBy` recorre el registro una vez, $O(n)$. Para cada región con $m_g$ sismos, `size` y `map(_.magnitud).max` cuestan $O(m_g)$, y la suma sobre todas las regiones es $O(n)$. Para cada región, `replicasSensibles` genera $j_g + 1$ elementos, donde $j_g$ es la cantidad de réplicas, y los cuenta con `size`, con costo $O(j_g)$. La cantidad $j_g$ depende solo de la magnitud máxima y de `minimo`, no del tamaño del registro. Con $k$ regiones, el costo total es $O\big(n + \sum_g j_g\big)$, que para magnitudes y umbrales razonables se comporta como $O(n)$.