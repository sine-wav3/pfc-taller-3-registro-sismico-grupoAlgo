# Informe de corrección: Taller 3, registro sísmico

Este informe argumenta, con notación matemática, que las funciones establecidas para el taller cumplan con sus requisitos. Para cada una se enuncia la especificación, se demuestra que la implementación la satisface y se estudia si el resultado depende del orden en que se recorre el registro.

## 0. Notación

Una lista finita se escribe $L = [x_1, x_2, \dots, x_m]$ con $m \ge 0$. La lista vacía es $[]$, el operador $x :: L$ agrega $x$ al inicio de $L$ y $L_1 \mathbin{+\!+} L_2$ es la concatenación.

El registro es $R = [s_1, \dots, s_n]$ con $n \ge 0$. De cada sismo $s$ interesan dos atributos:

- $\mathrm{reg}(s)$: la región del sismo.
- $\mathrm{mag}(s) \in \mathbb{R}$: la magnitud del sismo.

Definiciones auxiliares:

- $\mathrm{Reg}(R) = \{\mathrm{reg}(s) : s \in R\}$ es el conjunto de regiones presentes en $R$.
- $R_g = [s \in R : \mathrm{reg}(s) = g]$ es la sublista de los sismos de la región $g$, conservando el orden de $R$.
- $\mathrm{prefijo}_t(R) = [s_1,\dots,s_t]$ es el prefijo de los primeros $t$ elementos.

### 0.1. Semántica de las operaciones usadas

Se toman como definición las ecuaciones por casos de cada operación sobre listas:

$$\mathrm{map}(f)([]) = [] \qquad \mathrm{map}(f)(x :: L) = f(x) :: \mathrm{map}(f)(L)$$

$$\mathrm{filt}(p)([]) = [] \qquad \mathrm{filt}(p)(x :: L) = \begin{cases} x :: \mathrm{filt}(p)(L) & \text{si } p(x) \\ \mathrm{filt}(p)(L) & \text{si no} \end{cases}$$

$$\mathrm{flatMap}(f)([]) = [] \qquad \mathrm{flatMap}(f)(x :: L) = f(x) \mathbin{+\!+} \mathrm{flatMap}(f)(L)$$

La operación `withFilter` de Scala produce el mismo resultado que $\mathrm{filt}$ cuando va seguida de `map` o `flatMap`, y la desazucaración de `for` se traduce según:

$$\texttt{for \{ x <- L; if p(x) \} yield f(x)} \;=\; \mathrm{map}(f)\big(\mathrm{filt}(p)(L)\big)$$

$$\texttt{for \{ x <- A; y <- B; if q(x,y) \} yield h(x,y)} \;=\; \mathrm{flatMap}\big(x \mapsto \mathrm{map}(y \mapsto h(x,y))(\mathrm{filt}(y \mapsto q(x,y))(B))\big)(A)$$

La notación de comprensión de listas que se usa en las especificaciones es:

$$[f(x) \mid x \leftarrow L,\ p(x)] \;=\; \mathrm{map}(f)\big(\mathrm{filt}(p)(L)\big)$$

es decir, la lista de las imágenes por $f$ de los elementos de $L$ que cumplen $p$, en el orden de $L$.

## 1. Lemas generales

### Lema 1 (guardas encadenadas)

Para toda lista $L$ y predicados $p$, $q$:

$$\mathrm{filt}(q)\big(\mathrm{filt}(p)(L)\big) = \mathrm{filt}(x \mapsto p(x) \wedge q(x))(L)$$

**Demostración.** Por inducción sobre la longitud de $L$.

*Caso base*, $L = []$: ambos lados son $[]$.

*Paso inductivo*, $L = x :: L'$, suponiendo el resultado para $L'$:

Si $p(x)$ es falso, $\mathrm{filt}(p)(L) = \mathrm{filt}(p)(L')$, y por hipótesis de inducción el lado izquierdo es $\mathrm{filt}(p \wedge q)(L')$. El lado derecho también es $\mathrm{filt}(p \wedge q)(L')$, porque $p(x) \wedge q(x)$ es falso.

Si $p(x)$ es verdadero, $\mathrm{filt}(p)(L) = x :: \mathrm{filt}(p)(L')$. Si $q(x)$ es verdadero, el lado izquierdo es $x :: \mathrm{filt}(q)(\mathrm{filt}(p)(L'))$, que por hipótesis es $x :: \mathrm{filt}(p \wedge q)(L')$, igual al lado derecho. Si $q(x)$ es falso, el lado izquierdo es $\mathrm{filt}(q)(\mathrm{filt}(p)(L'))$, igual a $\mathrm{filt}(p \wedge q)(L')$, que es el lado derecho.

Queda demostrado el lema.

### Lema 2 (`for` con una guarda)

Para toda lista $L$, predicado $p$ y función $f$, la expresión `for { x <- L; if p(x) } yield f(x)` es la lista

$$[f(x_i) : 1 \le i \le m,\ p(x_i)]$$

con $[x_1,\dots,x_m] = L$. En palabras: contiene $f(x_i)$ exactamente para los $x_i$ que cumplen $p$, una vez por cada uno, en el mismo orden que en $L$.

**Demostración.** Por la desazucaración, la expresión es $\mathrm{map}(f)(\mathrm{filt}(p)(L))$. Se razona por inducción sobre $m$.

*Caso base*, $m = 0$: $\mathrm{filt}(p)([]) = []$ y $\mathrm{map}(f)([]) = []$. La lista de la derecha, indexada sobre un rango vacío, también es vacía.

*Paso inductivo*, $L = x_1 :: L'$ con $L' = [x_2,\dots,x_m]$, suponiendo el resultado para $L'$:

Si $p(x_1)$ es verdadero, $\mathrm{map}(f)(\mathrm{filt}(p)(L)) = f(x_1) :: \mathrm{map}(f)(\mathrm{filt}(p)(L'))$. Por hipótesis de inducción, el resto es la lista de las imágenes de los elementos de $L'$ que cumplen $p$, así que el total es la lista deseada, que empieza por $f(x_1)$.

Si $p(x_1)$ es falso, el resultado es $\mathrm{map}(f)(\mathrm{filt}(p)(L'))$, que por hipótesis es la lista deseada, y $x_1$ no aporta nada, como corresponde.

Queda demostrado el lema.

### Lema 3 (`for` con dos generadores y una guarda)

Para listas $A = [a_1,\dots,a_p]$ y $B = [b_1,\dots,b_q]$, un predicado $c(x,y)$ y una función $h$, la expresión

`for { x <- A; y <- B; if c(x,y) } yield h(x,y)`

es la concatenación, en el orden de $a_1, a_2, \dots, a_p$, de las listas

$$\big[h(a_i, b_j) : 1 \le j \le q,\ c(a_i, b_j)\big]$$

Es decir, los elementos salen ordenados primero por la posición $i$ en $A$ y, para igual $i$, por la posición $j$ en $B$ (orden lexicográfico de $(i,j)$), y aparece exactamente un elemento por cada par $(i,j)$ que cumple $c$.

**Demostración.** Por la desazucaración, la expresión es $\mathrm{flatMap}(g)(A)$ con $g(x) = \mathrm{map}(y \mapsto h(x,y))(\mathrm{filt}(y \mapsto c(x,y))(B))$. Por el Lema 2, $g(a_i)$ es exactamente la lista $[h(a_i, b_j) : 1 \le j \le q,\ c(a_i,b_j)]$. Por la ecuación de $\mathrm{flatMap}$, aplicada por inducción sobre $p$, se tiene $\mathrm{flatMap}(g)(A) = g(a_1) \mathbin{+\!+} g(a_2) \mathbin{+\!+} \cdots \mathbin{+\!+} g(a_p)$, que es la concatenación enunciada. Queda demostrado el lema.

### Lema 4 (`groupBy` conserva los elementos por llave)

Sea $k$ una función de llave. Para toda lista $R$, el resultado de `R.groupBy(k)` es un mapa $M$ tal que:

1. El dominio de $M$ es $\{k(s) : s \in R\}$.
2. Para toda llave $g$ del dominio, $M(g) = [s \in R : k(s) = g]$, es decir, los elementos de $R$ con llave $g$, en el orden en que aparecen en $R$.

**Demostración.** Se razona sobre el estado del mapa $M_t$ después de procesar los primeros $t$ elementos de $R$, por inducción sobre $t$. La hipótesis es que $M_t$ cumple 1 y 2 respecto del prefijo de longitud $t$.

*Caso base*, $t = 0$: el mapa está vacío, y el prefijo vacío no tiene llaves ni elementos.

*Paso inductivo*: al procesar $s_{t+1}$ con llave $g' = k(s_{t+1})$, el mapa $M_{t+1}$ se obtiene de $M_t$ agregando $s_{t+1}$ al final de la lista de $g'$ (creándola si no existía) y dejando las demás llaves iguales. Entonces:

- El dominio de $M_{t+1}$ es el de $M_t$ unido con $\{g'\}$, que es el conjunto de llaves del prefijo de longitud $t+1$.
- Para $g \ne g'$, $M_{t+1}(g) = M_t(g)$, que por hipótesis son los elementos del prefijo de llave $g$. Como $s_{t+1}$ no tiene llave $g$, siguen siendo los del nuevo prefijo.
- Para $g'$, $M_{t+1}(g') = M_t(g') \mathbin{+\!+} [s_{t+1}]$, es decir, los elementos anteriores de llave $g'$ en su orden, seguidos de $s_{t+1}$, que es el último elemento del nuevo prefijo. Luego conserva el orden.

Con $t = n$ se obtiene el enunciado. Queda demostrado el lema.

### Lema 5 (`groupMapReduce`)

Sean $k$ una función de llave, $v$ una función de valor y $\oplus$ un operador binario. Para toda lista $R$, el resultado de `R.groupMapReduce(k)(v)(⊕)` es un mapa $A$ tal que:

1. El dominio de $A$ es $\{k(s) : s \in R\}$.
2. Para toda llave $g$ del dominio, sea $[t_1,\dots,t_m] = [s \in R : k(s) = g]$ con $m \ge 1$. Entonces

$$A(g) = \big(\cdots\big((v(t_1) \oplus v(t_2)) \oplus v(t_3)\big) \cdots \oplus v(t_m)\big)$$

Si además $\oplus$ es asociativo, esto es $v(t_1) \oplus v(t_2) \oplus \cdots \oplus v(t_m)$ sin importar los paréntesis.

**Demostración.** Se razona sobre el estado $A_t$ después de procesar los primeros $t$ elementos, por inducción sobre $t$.

*Caso base*, $t = 0$: el mapa está vacío.

*Paso inductivo*: al procesar $s_{t+1}$ con llave $g' = k(s_{t+1})$:

- Si $g'$ no estaba en el dominio de $A_t$, se crea la entrada $A_{t+1}(g') = v(s_{t+1})$, que corresponde a un grupo con $m = 1$ elemento.
- Si $g'$ ya estaba, $A_{t+1}(g') = A_t(g') \oplus v(s_{t+1})$, que extiende la expresión por la derecha con el nuevo valor, como pide el enunciado.
- Las demás llaves no cambian.

Cada entrada existe solo si se procesó al menos un elemento con esa llave, por lo que $m \ge 1$ en todo grupo y la reducción nunca se aplica a una colección vacía. Con $t = n$ se obtiene el enunciado. Queda demostrado el lema.

### Lema 6 (independencia del orden de una reducción)

Sea $\oplus$ un operador binario asociativo y conmutativo, y sea $[y_1,\dots,y_m]$ una lista con $m \ge 1$. Para toda permutación $\sigma$ de $\{1,\dots,m\}$:

$$y_{\sigma(1)} \oplus \cdots \oplus y_{\sigma(m)} = y_1 \oplus \cdots \oplus y_m$$

**Demostración.** Toda permutación se obtiene como una sucesión de intercambios de elementos adyacentes, así que basta probar que intercambiar dos elementos adyacentes no cambia el valor. Considérese una reducción hacia la izquierda $\big(\cdots(y_1 \oplus y_2) \cdots\big) \oplus y_m$ y supóngase que se intercambian $y_i$ y $y_{i+1}$. Sea $a$ el valor acumulado de los primeros $i-1$ elementos. Entonces

$$(a \oplus y_i) \oplus y_{i+1} = a \oplus (y_i \oplus y_{i+1}) = a \oplus (y_{i+1} \oplus y_i) = (a \oplus y_{i+1}) \oplus y_i$$

por asociatividad, conmutatividad y asociatividad de nuevo. El acumulado después de esos dos pasos es el mismo, y los pasos siguientes parten del mismo valor. Luego el resultado final es igual. Queda demostrado el lema.

## 2. Punto 1

### 2.1. Corrección de `fuertes`

**Especificación.** Dado un umbral $u \in \mathbb{R}$, `fuertes` debe devolver las regiones de los sismos cuya magnitud supera $u$, en el orden en que aparecen en el registro y sin quitar repetidos:

$$\mathrm{fuertes}(R,u) = [\mathrm{reg}(s_i) : 1 \le i \le n,\ \mathrm{mag}(s_i) > u]$$

**Argumento.** La implementación es

```scala
for { s <- r; if s.magnitud > umbral } yield s.region
```

que es una expresión del tipo del Lema 2 con $L = R$, $p(s) = (\mathrm{mag}(s) > u)$ y $f = \mathrm{reg}$. Por el Lema 2, el resultado contiene $\mathrm{reg}(s_i)$ exactamente para los $s_i$ con $\mathrm{mag}(s_i) > u$, uno por cada sismo que cumple y en el orden de $R$. No interviene ninguna operación que elimine repetidos, así que si dos sismos que cumplen tienen la misma región, esa región aparece dos veces. Esto coincide con la especificación.

**Casos borde.** Si $R = []$, el resultado es $[]$ (caso base del Lema 2). Si $u$ es mayor o igual que toda magnitud, ningún elemento cumple $p$ y el resultado es $[]$. La desigualdad es estricta, por lo que un sismo con magnitud exactamente $u$ no se incluye. Queda demostrada la corrección.

### 2.2. Corrección de `enRegion`

**Especificación.** Dada una región $g$:

$$\mathrm{enRegion}(R,g) = [\mathrm{mag}(s_i) : 1 \le i \le n,\ \mathrm{reg}(s_i) = g]$$

**Argumento.** La implementación es

```scala
for { s <- r; if s.region == region } yield s.magnitud
```

que es el Lema 2 con $p(s) = (\mathrm{reg}(s) = g)$ y $f = \mathrm{mag}$. Por el Lema 2 el resultado es exactamente la lista de la especificación. Equivale a $\mathrm{map}(\mathrm{mag})(R_g)$, es decir, las magnitudes de los sismos de la región $g$ en el orden del registro.

**Casos borde.** Si $g \notin \mathrm{Reg}(R)$, ningún sismo cumple $p$ y el resultado es $[]$, que es lo que pide el enunciado para una región ausente, sin lanzar excepción. Si $R = []$ el resultado es $[]$. Queda demostrada la corrección.

### 2.3. Corrección de `paresCercanos`

**Definiciones.** Sea $\delta \ge 0$ el parámetro `dif`. Para dos regiones $a$ y $b$ se define el predicado de cercanía:

$$C(a,b) \iff \exists\, s, s' \in R :\ \mathrm{reg}(s) = a \wedge \mathrm{reg}(s') = b \wedge \lvert \mathrm{mag}(s) - \mathrm{mag}(s') \rvert \le \delta$$

Sea $G = [g_0, g_1, \dots, g_{k-1}]$ la lista de las regiones distintas de $R$ en orden de primera aparición. Es el resultado de `r.map(_.region).distinct`: `distinct` conserva la primera aparición de cada elemento y elimina las siguientes, de modo que los $g_i$ son todos distintos entre sí y $\{g_0,\dots,g_{k-1}\} = \mathrm{Reg}(R)$.

**Especificación.** El resultado debe contener las parejas de regiones distintas con algún sismo de magnitudes a lo sumo $\delta$ de distancia, cada pareja una sola vez sin importar el orden, y con las regiones en el orden en que aparecen en el registro:

$$\mathrm{paresCercanos}(R,\delta) = \big[(g_i, g_j) : 0 \le i < j < k,\ C(g_i,g_j)\big]$$

listada en orden lexicográfico de $(i,j)$.

**Argumento.**

*Paso 1: forma del `for`.* Sea $Z = [(g_0,0), (g_1,1), \dots, (g_{k-1},k-1)]$ el resultado de `.zipWithIndex` sobre $G$. Los dos generadores recorren $Z$. Las dos guardas `if i < j` e `if r.exists(...)` son equivalentes, por el Lema 1, a una única guarda $c\big((a,i),(b,j)\big) = (i < j) \wedge E(a,b)$, donde $E(a,b)$ es el valor de la expresión `r.exists(...)`. La expresión es entonces del tipo del Lema 3, con $A = B = Z$ y $h\big((a,i),(b,j)\big) = (a,b)$.

*Paso 2: la guarda de cercanía implementa $C$.* La expresión

```scala
r.exists(s1 => s1.region == a &&
  r.exists(s2 => s2.region == b &&
    Math.abs(s1.magnitud - s2.magnitud) <= dif))
```

es verdadera si y solo si existe $s_1 \in R$ con $\mathrm{reg}(s_1) = a$ tal que existe $s_2 \in R$ con $\mathrm{reg}(s_2) = b$ y $\lvert \mathrm{mag}(s_1) - \mathrm{mag}(s_2) \rvert \le \delta$. Dos cuantificadores existenciales anidados equivalen a uno doble, por lo que $E(a,b)$ es verdadero exactamente cuando $C(a,b)$ lo es.

*Paso 3: aplicación del Lema 3.* Por el Lema 3, el resultado es la concatenación, para $i = 0,1,\dots,k-1$ en ese orden, de las listas $[(g_i,g_j) : 0 \le j < k,\ i < j,\ C(g_i,g_j)]$, con $j$ creciente dentro de cada una. Esto es exactamente la lista de la especificación, en orden lexicográfico de $(i,j)$.

*Paso 4: regiones distintas.* Como los $g_i$ son todos distintos, la condición $i < j$ implica $g_i \ne g_j$. Luego toda pareja producida es de regiones distintas.

*Paso 5: cada pareja sale una sola vez.* Cada pareja de índices $(i,j)$ aparece a lo sumo una vez, por el Lema 3. Dos parejas de índices distintas $(i,j) \ne (i',j')$ con $i<j$ e $i'<j'$ dan parejas de regiones distintas como pares ordenados, y tampoco coinciden como pares no ordenados: de $\{g_i,g_j\} = \{g_{i'},g_{j'}\}$ y la condición $i<j$, $i'<j'$ se sigue $(i,j) = (i',j')$, porque los $g$ son todos distintos. Por tanto, si una pareja salió como $(g_i, g_j)$, no vuelve a salir como $(g_j, g_i)$, que tendría $j > i$ en la primera posición y no pasaría la guarda.

*Paso 6: no se pierde ninguna pareja válida.* El predicado $C$ es simétrico:

$$C(a,b) \iff C(b,a)$$

En efecto, si $s, s'$ son los testigos de $C(a,b)$, entonces $s', s$ son los testigos de $C(b,a)$, porque $\lvert x - y \rvert = \lvert y - x \rvert$. Entonces, para toda pareja no ordenada $\{g_i, g_j\}$ con $i \ne j$ y $C(g_i,g_j)$, la orientación con el menor índice primero también cumple $C$, y se incluye en el resultado.

*Paso 7: orden de las regiones en cada pareja.* Cada pareja produce primero la región de menor índice, es decir, la que aparece antes en el registro. Así, si `"Valle"` aparece antes que `"Choco"`, la pareja es `("Valle", "Choco")` y nunca `("Choco", "Valle")`.

**Casos borde.** Si $R = []$, entonces $G = []$, $Z = []$ y el resultado es $[]$. Si $R$ tiene una sola región, $k = 1$ y no existen índices con $i < j$, luego el resultado es $[]$.

**Verificación con el ejemplo del enunciado.** Con $G = [\text{Valle}, \text{Santander}, \text{Choco}]$ y las menores diferencias entre magnitudes de regiones distintas, $0.2$ para (Valle, Santander), $0.7$ para (Valle, Choco) y $0.9$ para (Santander, Choco):

- Con $\delta = 0.6$ solo cumple $C(\text{Valle},\text{Santander})$.
- Con $\delta = 1.0$ cumplen las tres, en el orden lexicográfico $(0,1)$, $(0,2)$, $(1,2)$.
- Con $\delta = 0.1$ no cumple ninguna y el resultado es $[]$.

**Observación sobre punto flotante.** La comparación $\le \delta$ se hace con números `Double`. Cuando la diferencia exacta en decimal es igual a $\delta$, el error de representación puede alterar el resultado de la comparación. La demostración anterior es válida sobre los reales; sobre `Double` es válida salvo en esos casos frontera. Las pruebas deben elegir valores que no dependan de la igualdad exacta.

Queda demostrada la corrección.

### 2.4. Independencia del orden de recorrido en el Punto 1

Sea $R^\pi$ el registro obtenido al aplicar una permutación $\pi$ a $R$.

**`fuertes` y `enRegion`.** Por el Lema 2, cada elemento de $R$ aporta su imagen al resultado si y solo si cumple el predicado, y esa decisión depende solo del elemento mismo, no de su posición ni de sus vecinos. Por tanto, el multiconjunto de elementos del resultado es el mismo para $R$ y para $R^\pi$. Lo único que cambia al permutar es el orden de la lista resultante, que sigue el orden del registro. Esto no es un defecto: el enunciado especifica "en el orden en que aparecen", por lo que el orden de la salida es parte de la especificación y se deriva del orden de la entrada de forma determinista.

**`paresCercanos`.** El predicado $C(a,b)$ está definido con un cuantificador existencial sobre $R$, y un $\exists$ no depende del orden en que se examinan los elementos. Por tanto, el conjunto de parejas no ordenadas $\{\{a,b\} : a \ne b,\ C(a,b)\}$ es el mismo para $R$ y para $R^\pi$. Lo que sí cambia al permutar es la lista $G$, y con ella el orden de listado y la orientación de cada pareja. Ambos están determinados por el orden de primera aparición, que la especificación define como criterio.

## 3. Punto 2

### 3.1. Corrección de `porRegion`

**Especificación.**

$$\mathrm{dom}\big(\mathrm{porRegion}(R)\big) = \mathrm{Reg}(R), \qquad \mathrm{porRegion}(R)(g) = R_g \ \text{ para todo } g \in \mathrm{Reg}(R)$$

**Argumento.** La implementación es `r.groupBy(_.region)`. Por el Lema 4 con $k = \mathrm{reg}$, el dominio del mapa es $\{\mathrm{reg}(s) : s \in R\} = \mathrm{Reg}(R)$ y la lista asociada a cada región $g$ contiene los sismos de $R$ con región $g$, en el orden del registro, que es $R_g$. Una región que no está en el registro no es llave del mapa, lo que cumple la condición del enunciado de que las regiones ausentes no aparecen. Si $R = []$ el mapa es vacío. Queda demostrada la corrección.

### 3.2. Corrección de `conteoPorRegion`

**Especificación.**

$$\mathrm{conteoPorRegion}(R)(g) = \lvert R_g \rvert \ \text{ para todo } g \in \mathrm{Reg}(R), \qquad \mathrm{dom} = \mathrm{Reg}(R)$$

**Argumento.** La implementación es `r.groupMapReduce(_.region)(_ => 1)(_ + _)`. Se aplica el Lema 5 con $k = \mathrm{reg}$, $v(s) = 1$ y $\oplus = +$. La suma de enteros es asociativa. Para $g \in \mathrm{Reg}(R)$ con $[t_1,\dots,t_m] = R_g$:

$$\mathrm{conteoPorRegion}(R)(g) = \underbrace{1 + 1 + \cdots + 1}_{m \text{ sumandos}} = m = \lvert R_g \rvert$$

El dominio es $\mathrm{Reg}(R)$ por el Lema 5, por lo que las regiones ausentes no aparecen, y si $R = []$ el resultado es el mapa vacío.

**Consistencia con `porRegion`.** Para toda $g \in \mathrm{Reg}(R)$ se cumple $\mathrm{conteoPorRegion}(R)(g) = \lvert \mathrm{porRegion}(R)(g) \rvert$, pues ambos valores son $\lvert R_g \rvert$. Además, la suma de todos los conteos es $\sum_{g \in \mathrm{Reg}(R)} \lvert R_g \rvert = n$, porque los conjuntos $R_g$ forman una partición de los elementos de $R$.

**Sobre el desbordamiento de `Int`.** La suma de `Int` en la JVM es aritmética módulo $2^{32}$, que sigue siendo asociativa y conmutativa. Además, ningún conteo supera $n$, que es el tamaño de una lista en memoria, por lo que en la práctica no hay desbordamiento. Queda demostrada la corrección.

### 3.3. Corrección de `magnitudMaxima`

**Especificación.**

$$\mathrm{magnitudMaxima}(R)(g) = \max\{\mathrm{mag}(s) : s \in R_g\} \ \text{ para todo } g \in \mathrm{Reg}(R), \qquad \mathrm{dom} = \mathrm{Reg}(R)$$

**Argumento.** La implementación es `r.groupMapReduce(_.region)(_.magnitud)(math.max)`. Se aplica el Lema 5 con $k = \mathrm{reg}$, $v = \mathrm{mag}$ y $\oplus = \max$. El operador $\max$ es asociativo:

$$\max(a, \max(b,c)) = \max(\max(a,b), c)$$

pues ambos lados son el mayor de los tres valores. Por el Lema 5, para $[t_1,\dots,t_m] = R_g$:

$$\mathrm{magnitudMaxima}(R)(g) = \max\big(\mathrm{mag}(t_1), \dots, \mathrm{mag}(t_m)\big) = \max\{\mathrm{mag}(s) : s \in R_g\}$$

Como $m \ge 1$ en todo grupo, el máximo existe y no se lanza excepción por reducir una colección vacía. Si $R = []$, el resultado es el mapa vacío.

**Exactitud numérica.** $\max(x,y)$ devuelve uno de sus dos argumentos sin modificarlo, por lo que el valor entregado es exactamente una de las magnitudes del registro y no acumula error de redondeo. (Se supone que ninguna magnitud es `NaN`, caso que el modelo de datos del taller no contempla.) Queda demostrada la corrección.

### 3.4. Independencia del orden de recorrido en el Punto 2

Sea $R^\pi$ una permutación cualquiera de $R$.

**`conteoPorRegion` y `magnitudMaxima`.** Por el Lema 5, el valor de cada llave es una reducción de los valores del grupo. Los operadores $+$ sobre los enteros y $\max$ sobre los reales son asociativos y conmutativos. Como el grupo de la región $g$ en $R^\pi$ contiene los mismos sismos que en $R$, pero posiblemente en otro orden, el Lema 6 asegura que la reducción da el mismo valor. Por tanto:

$$\mathrm{conteoPorRegion}(R^\pi) = \mathrm{conteoPorRegion}(R), \qquad \mathrm{magnitudMaxima}(R^\pi) = \mathrm{magnitudMaxima}(R)$$

Esta propiedad es más fuerte que en el Punto 1: el resultado completo no depende ni del orden del registro ni de la forma de parentizar la reducción. Es la propiedad que haría posible calcular los grupos por partes, incluso en paralelo, y combinar los resultados parciales.

**`porRegion`.** Por el Lema 4, la lista asociada a $g$ contiene los sismos de la región $g$ en el orden en que aparecen. Al permutar $R$, el multiconjunto de sismos de cada región es el mismo, y lo único que cambia es el orden interno de cada lista, que por especificación sigue el orden de la entrada. Los dominios de ambos mapas coinciden, pues dependen solo de $\mathrm{Reg}(R)$.

**Orden de las llaves.** La igualdad entre mapas no depende del orden de inserción de las llaves, y el orden en que un `Map` las presenta no forma parte de la especificación. Por eso los resultados de `conteoPorRegion` y `magnitudMaxima` son iguales como mapas para cualquier permutación del registro.

## 4. Punto 3: Corrección de `consultas seguras`

### Notación

Una lista finita se escribe $L = [x_1, x_2, \dots, x_m]$ con $m \ge 0$. El registro es $R = [s_1, \dots, s_n]$ con $n \ge 0$. De cada sismo $s$ interesan:

- $\mathrm{reg}(s)$: la región del sismo.
- $\mathrm{mag}(s) \in \mathbb{R}$: la magnitud del sismo.

Definiciones auxiliares:

- $\mathrm{Reg}(R) = \{\mathrm{reg}(s) : s \in R\}$ es el conjunto de regiones presentes en $R$.
- $R_g = [s \in R : \mathrm{reg}(s) = g]$ es la sublista de los sismos de la región $g$, conservando el orden de $R$.

### Lemas usados en el Punto 3

Además de los resultados del informe anterior (Lema 4 sobre `groupBy`, Lema 6 sobre independencia del orden de una reducción, y los Puntos 2.2 y 2.3), este punto usa los siguientes lemas.

#### Lema 7 (`map` sobre las entradas de un mapa)

Sea $M$ un mapa con llaves distintas y sea $F(g, v) = (g, \varphi(g,v))$ una función que conserva la llave. Entonces `M.map(F)` es un mapa $M'$ tal que:

1. $\mathrm{dom}(M') = \mathrm{dom}(M)$.
2. $M'(g) = \varphi(g, M(g))$ para todo $g \in \mathrm{dom}(M)$.

**Demostración.** `M.map(F)` aplica $F$ a cada entrada y construye un mapa con los pares resultantes. Como las llaves de $M$ son todas distintas y $F$ no las modifica, las llaves de los pares resultantes también son todas distintas. Por tanto, ninguna entrada sobrescribe a otra y cada una sobrevive con su llave original y el valor $\varphi(g, M(g))$. De ahí se siguen 1 y 2. Queda demostrado el lema.

#### Lema 8 (`withDefaultValue`)

Sea $M$ un mapa y $d$ un valor. Entonces `M.withDefaultValue(d)` es un mapa $M_d$ tal que:

1. $\mathrm{dom}(M_d) = \mathrm{dom}(M)$ (las llaves no cambian).
2. $M_d(g) = M(g)$ si $g \in \mathrm{dom}(M)$, y $M_d(g) = d$ si $g \notin \mathrm{dom}(M)$.

**Demostración.** `withDefaultValue` envuelve al mapa original sin copiar ni agregar entradas. Solo redefine el comportamiento de `apply` para las llaves ausentes, que en lugar de lanzar `NoSuchElementException` devuelve $d$. Por eso `contains` y `get` siguen reportando como ausentes las llaves que no están en $M$. Queda demostrado el lema.

#### Lema 9 (`maxOption` de una lista)

Sea $[y_1,\dots,y_m]$ una lista de reales sin `NaN`.

1. Si $m \ge 1$, `max` devuelve un elemento $y_i$ tal que $y_j \le y_i$ para todo $j$. Es decir, devuelve $\max\{y_1,\dots,y_m\}$.
2. `maxOption` devuelve `None` si $m = 0$ y `Some(max)` si $m \ge 1$.

**Demostración.** La parte 2 es la definición de `maxOption`. Para la parte 1, `max` es una reducción hacia la izquierda con el operador $\max$, y se razona por inducción sobre $m$.

*Caso base*, $m = 1$: el resultado es $y_1$, que es mayor o igual que todos los elementos de la lista, pues es el único.

*Paso inductivo*: sea $a$ el resultado para $[y_1,\dots,y_m]$, que por hipótesis es mayor o igual que cada $y_j$ con $j \le m$. Al agregar $y_{m+1}$ se calcula $\max(a, y_{m+1})$, que devuelve el mayor de los dos y es uno de los elementos de la lista. Es mayor o igual que $a$, y por tanto que todos los $y_j$ con $j \le m$, y también que $y_{m+1}$.

#### Lema 10 (`sortBy` con llaves distintas)

Sea $L$ una lista de elementos y $\kappa$ una función de llave a un orden total, tal que $\kappa(x) \ne \kappa(y)$ para elementos distintos de $L$. Entonces `L.sortBy(κ)` es una permutación de $L$ en la que las llaves son estrictamente crecientes, y esa lista es la misma para cualquier ordenamiento inicial de $L$. En particular, su cabeza es el único elemento de $L$ con llave mínima.

**Demostración.** `sortBy` devuelve una permutación de $L$ con las llaves en orden no decreciente. Como las llaves son todas distintas, el orden no decreciente es estrictamente creciente. Un conjunto finito de llaves distintas en un orden total tiene un único ordenamiento creciente, por lo que el resultado no depende del orden inicial de $L$. La cabeza de esa lista tiene la llave más pequeña, y es única por ser las llaves distintas. Queda demostrado el lema.

### Corrección de `conteoSeguro`

**Especificación.**

$$\mathrm{conteoSeguro}(R)(g) = \lvert R_g \rvert \quad \text{para todo } g \in \mathrm{Reg}(R)$$

$$\mathrm{conteoSeguro}(R)(g) = 0 \quad \text{para todo } g \notin \mathrm{Reg}(R)$$

La llamada a `apply` no debe lanzar excepción para ninguna región.

**Argumento.** La implementación es

```scala
r.groupBy(_.region).map { case (region, sismos) =>
  region -> sismos.size
}.withDefaultValue(0)
```

*Paso 1: agrupación.* Por el Lema 4 con $k = \mathrm{reg}$, `r.groupBy(_.region)` es un mapa $M$ con $\mathrm{dom}(M) = \mathrm{Reg}(R)$ y $M(g) = R_g$.

*Paso 2: conteo.* El `map` aplica $F(g, L) = (g, \lvert L \rvert)$, que conserva la llave. Por el Lema 7, el mapa resultante $M'$ cumple $\mathrm{dom}(M') = \mathrm{Reg}(R)$ y $M'(g) = \lvert M(g) \rvert = \lvert R_g \rvert$.

*Paso 3: valor por defecto.* Por el Lema 8 con $d = 0$, el mapa final cumple $\mathrm{conteoSeguro}(R)(g) = M'(g) = \lvert R_g \rvert$ si $g \in \mathrm{Reg}(R)$, y vale $0$ si $g \notin \mathrm{Reg}(R)$. Ese segundo caso no lanza excepción, porque `apply` devuelve el valor por defecto en lugar de fallar.

**Casos borde.** Si $R = []$, el mapa $M'$ es vacío y toda consulta devuelve $0$. Una región que existe en el registro pero con un solo sismo devuelve $1$.

**Consistencia con `conteoPorRegion`.** Para todo $g \in \mathrm{Reg}(R)$ se cumple $\mathrm{conteoSeguro}(R)(g) = \mathrm{conteoPorRegion}(R)(g)$, pues ambos son $\lvert R_g \rvert$ (Punto 2.2). La única diferencia es que `conteoSeguro` está definido también fuera de $\mathrm{Reg}(R)$, donde vale $0$. Esto es coherente con el hecho de que $\lvert R_g \rvert = 0$ si y solo si $g \notin \mathrm{Reg}(R)$, por lo que la función es $g \mapsto \lvert R_g \rvert$ para toda región $g$.

**Observación.** El valor por defecto no agrega llaves al mapa: `contains(g)` y `get(g)` siguen reportando como ausentes las regiones que no están en $R$, y solo `apply` devuelve $0$. Esto es consistente con la especificación, que habla del valor de la consulta y no del dominio. Queda demostrada la corrección.

### Corrección de `maximaDe`

**Especificación.**

$$\mathrm{maximaDe}(R, g) = \begin{cases} \mathrm{Some}\big(\max\{\mathrm{mag}(s) : s \in R_g\}\big) & \text{si } g \in \mathrm{Reg}(R) \\ \mathrm{None} & \text{si } g \notin \mathrm{Reg}(R) \end{cases}$$

**Argumento.** La implementación es

```scala
r.filter(_.region == region).map(_.magnitud).maxOption
```

*Paso 1: filtro.* Por la definición de $\mathrm{filt}$, `r.filter(_.region == region)` es la lista $R_g$ de los sismos de la región $g$ en el orden del registro.

*Paso 2: proyección.* `map(_.magnitud)` produce la lista $[\mathrm{mag}(t_1),\dots,\mathrm{mag}(t_m)]$ con $[t_1,\dots,t_m] = R_g$, de longitud $m = \lvert R_g \rvert$.

*Paso 3: máximo opcional.* Se distinguen dos casos:

- Si $g \in \mathrm{Reg}(R)$, existe algún $s \in R$ con $\mathrm{reg}(s) = g$, así que $m \ge 1$. Por el Lema 9, `maxOption` devuelve $\mathrm{Some}\big(\max\{\mathrm{mag}(t_i)\}\big)$, que es la especificación.
- Si $g \notin \mathrm{Reg}(R)$, ningún sismo cumple el filtro, luego $m = 0$ y por el Lema 9 `maxOption` devuelve `None`.

**Casos borde.** Si $R = []$, la consulta devuelve `None` para cualquier región. Nunca se lanza excepción, porque `maxOption` (a diferencia de `max`) está definido sobre la lista vacía.

**Consistencia con `magnitudMaxima`.** Para $g \in \mathrm{Reg}(R)$ se cumple $\mathrm{maximaDe}(R,g) = \mathrm{Some}\big(\mathrm{magnitudMaxima}(R)(g)\big)$ (Punto 2.3), y para $g \notin \mathrm{Reg}(R)$ la respuesta es `None`, donde `magnitudMaxima` no tiene entrada. El tipo `Option` es lo que hace explícita la ausencia.

**Exactitud numérica.** Como en el Punto 2.3, $\max$ devuelve uno de sus argumentos sin modificarlo, por lo que el valor entregado es exactamente una de las magnitudes del registro. Se supone que no hay `NaN`. Queda demostrada la corrección.

### Corrección de `regionMasActiva`

**Especificación.** Sea $\kappa(g) = (-\lvert R_g \rvert,\ g)$ para $g \in \mathrm{Reg}(R)$, ordenada lexicográficamente (primero por la primera componente y, en empate, por la segunda, con el orden natural de las cadenas). Entonces

$$
\mathrm{regionMasActiva}(R) = \begin{cases}
\mathrm{None} & \text{si } R = [] \\
\mathrm{Some}(g^{\star}) & \text{si } R \ne [],\ \text{con } g^{\star} \in \mathrm{Reg}(R) \text{ de } \kappa \text{ mínima}
\end{cases}
$$

Desarrollado, $g^{\star}$ cumple dos condiciones: $\lvert R_{g^{\star}} \rvert \ge \lvert R_g \rvert$ para todo $g \in \mathrm{Reg}(R)$, y entre las regiones con el conteo máximo, $g^{\star}$ es la menor en orden alfabético.

**Argumento.** La implementación es:

```scala
r.groupBy(_.region)
  .toList
  .sortBy { case (region, sismos) => (-sismos.size, region) }
  .headOption
  .map(_._1)
```

*Paso 1: la lista de entradas.* Por el Lema 4, `r.groupBy(_.region)` es un mapa con dominio $\mathrm{Reg}(R)$, y `toList` produce una lista con exactamente un par $(g, R_g)$ por cada $g \in \mathrm{Reg}(R)$. El orden de esta lista depende de la iteración interna del mapa y no forma parte de la especificación, pero no importa, como muestra el Paso 3.

*Paso 2: la llave.* La función de ordenamiento asigna al par $(g, R_g)$ la llave $(-\lvert R_g \rvert, g) = \kappa(g)$. Dos pares distintos tienen regiones distintas, así que sus llaves difieren al menos en la segunda componente. Por tanto, las llaves son todas distintas.

*Paso 3: ordenamiento.* Por el Lema 10, `sortBy` produce una lista única, con $\kappa$ estrictamente creciente y sin depender del orden de la lista inicial. Su cabeza es el par con $\kappa$ mínima.

*Paso 4: la cabeza.* Se distinguen dos casos:

- Si $R = []$, entonces $\mathrm{Reg}(R) = \emptyset$, la lista es vacía y `headOption` devuelve `None`.
- Si $R \ne []$, la lista es no vacía, `headOption` devuelve la cabeza $(g^*, R_{g^*})$ y `.map(_._1)` extrae $g^*$.

*Paso 5: lectura de la condición.* Como $\kappa(g^*) < \kappa(g)$ para todo $g \ne g^*$, o bien $-\lvert R_{g^*} \rvert < -\lvert R_g \rvert$, o bien los conteos son iguales y $g^* < g$. En ambos casos $\lvert R_{g^*} \rvert \ge \lvert R_g \rvert$, así que $g^*$ tiene el conteo máximo. Y entre las regiones con ese conteo máximo, $g^*$ es menor que todas las demás en orden alfabético. Esto es exactamente la especificación.

**Casos borde.** Si $R = []$ el resultado es `None`. Si hay una sola región, el resultado es `Some` de esa región. Si todas las regiones empatan en conteo, el resultado es la primera en orden alfabético. Como $\lvert R_g \rvert \ge 1$ para toda $g \in \mathrm{Reg}(R)$, la negación $-\lvert R_g \rvert$ no produce desbordamiento.

**Observación sobre el orden alfabético.** El orden natural de `String` en Scala es el lexicográfico por unidad de código, por lo que las mayúsculas anteceden a las minúsculas y las vocales con tilde quedan después de las letras sin tilde. Con nombres de región escritos de forma homogénea (por ejemplo, todos con la inicial en mayúscula) esto coincide con el orden alfabético esperado. Queda demostrada la corrección.

### Independencia del orden de recorrido en el Punto 3

Sea $R^\pi$ una permutación cualquiera de $R$.

**`conteoSeguro`.** El grupo de la región $g$ en $R^\pi$ contiene los mismos sismos que en $R$, posiblemente en otro orden, así que su tamaño es el mismo: $\lvert R^\pi_g \rvert = \lvert R_g \rvert$. Como el valor de cada consulta es ese tamaño (o $0$ si $g$ está ausente), se cumple $\mathrm{conteoSeguro}(R^\pi) = \mathrm{conteoSeguro}(R)$ en toda consulta.

**`maximaDe`.** La presencia de una región, $g \in \mathrm{Reg}(R)$, no depende del orden, y el máximo de un grupo tampoco, por el Lema 6 ($\max$ es asociativo y conmutativo). Por tanto, $\mathrm{maximaDe}(R^\pi, g) = \mathrm{maximaDe}(R, g)$ para toda región $g$.

**`regionMasActiva`.** El resultado depende solo de $\kappa(g) = (-\lvert R_g \rvert, g)$, que es invariante bajo permutaciones del registro. Por tanto, $\mathrm{regionMasActiva}(R^\pi) = \mathrm{regionMasActiva}(R)$. Además, el criterio de desempate alfabético es lo que hace determinista el resultado: sin él, un empate se resolvería según el orden de iteración del mapa, que no es parte de la especificación. Aquí el resultado es independiente tanto del orden del registro como del orden interno del mapa (Lema 10).

## 5. Punto 4: corrección de `replicas` y `replicasSensibles`

Sea $r_0 = \mathrm{red}(m)$ y $r_{k+1} = \mathrm{red}(0.8 \cdot r_k)$, con

$$\mathrm{red}(x) = \mathrm{round}(100x)/100.$$

Esta es la especificación. La secuencia es infinita y perezosa, así que la propiedad se enuncia sobre los primeros $n$ elementos, no sobre toda la secuencia.

### Teorema 1

$\forall k \in \mathbb{N} : \texttt{replicas}(m)(k) = r_k$.

*Caso base* $k = 0$: la cabeza de `iterate` es $\mathrm{red}(m) = r_0$.

*Paso inductivo*: por hipótesis, el elemento $k$ es $r_k$. `iterate` define el siguiente como $f(r_k) = \mathrm{red}(0.8 \cdot r_k) = r_{k+1}$.

### Teorema 2 (monotonía)

$r_{k+1} \le r_k$ para todo $k$.

Como $r_k \ge 0$ es múltiplo de $0.01$, $\mathrm{red}(r_k) = r_k$. Como $0.8 \cdot r_k \le r_k$ y $\mathrm{red}$ es monótona,

$$r_{k+1} = \mathrm{red}(0.8 \cdot r_k) \le \mathrm{red}(r_k) = r_k.$$

### Teorema 3

Si $\mathrm{min} \ge 0.02$, entonces

$$\texttt{replicasSensibles}(m, \mathrm{min}) = [r_0, \dots, r_{j-1}],$$

donde $j$ es el menor índice con $r_j \le \mathrm{min}$.

**Prueba.** Por el Teorema 2, si $r_j \le \mathrm{min}$ entonces $r_i \le \mathrm{min}$ para todo $i \ge j$, y por minimalidad de $j$, $r_i > \mathrm{min}$ para $i < j$. `takeWhile` toma exactamente los $r_i$ con $i < j$ y se detiene en $r_j$.

**Terminación.** Si $r_k \ge 0.03$, entonces $0.2 \cdot r_k > 0.005$, así que el redondeo no recupera la diferencia y $r_{k+1} \le r_k - 0.01$. La secuencia baja al menos $0.01$ por paso hasta llegar a $\le 0.02 \le \mathrm{min}$, luego $j$ existe.

**Observación.** Si $\mathrm{min} < 0.02$, la secuencia se estanca en $0.02$ ($\mathrm{red}(0.016) = 0.02$) y `replicasSensibles` no termina. Es una limitación del redondeo a dos decimales que pide el enunciado.

**Verificación con el informe.** Para $m = 6.0$, $\mathrm{min} = 3.0$:

$$r = 6.0,\ 4.8,\ 3.84,\ 3.07,\ 2.46,$$

así que $j = 4$ y la lista tiene 4 elementos. Coincide con `Resumen(2, 6.0, 4)` de Chocó.

## 6. Punto 5: corrección de `informe combinado`

### Lemas usados en el Punto 5

Además de los resultados del informe anterior (Lema 4 sobre `groupBy`, Lema 6 sobre independencia del orden de una reducción, Teorema 3 del Punto 4 y los Puntos "corrección de informe" e "independencia del orden de recorrido" del Punto 5), este punto usa los siguientes lemas. Son autónomos: no dependen de los lemas del Punto 3.

#### Lema 11 (`map` sobre las entradas de un mapa)

Sea $M$ un mapa con llaves distintas y sea $F(g, v) = (g, \varphi(g,v))$ una función que conserva la llave. Entonces `M.map(F)` es un mapa $M'$ tal que:

1. $\mathrm{dom}(M') = \mathrm{dom}(M)$.
2. $M'(g) = \varphi(g, M(g))$ para todo $g \in \mathrm{dom}(M)$.

**Demostración.** `M.map(F)` aplica $F$ a cada entrada y construye un mapa con los pares resultantes. Como las llaves de $M$ son todas distintas y $F$ no las modifica, las llaves de los pares resultantes también son todas distintas. Por tanto, ninguna entrada sobrescribe a otra y cada una sobrevive con su llave original y el valor $\varphi(g, M(g))$. De ahí se siguen 1 y 2. Queda demostrado el lema.

#### Lema 12 (`max` de una lista no vacía)

Sea $[y_1,\dots,y_m]$ una lista de reales sin `NaN` con $m \ge 1$. Entonces `max` devuelve un elemento $y_i$ tal que $y_j \le y_i$ para todo $j$, es decir, devuelve $\max\{y_1,\dots,y_m\}$. Si $m = 0$, `max` lanza una excepción.

**Demostración.** `max` es una reducción hacia la izquierda con el operador $\max$, y se razona por inducción sobre $m$.

*Caso base*, $m = 1$: el resultado es $y_1$, que es mayor o igual que todos los elementos de la lista, pues es el único.

*Paso inductivo*: sea $a$ el resultado para $[y_1,\dots,y_m]$, que por hipótesis es mayor o igual que cada $y_j$ con $j \le m$. Al agregar $y_{m+1}$ se calcula $\max(a, y_{m+1})$, que devuelve el mayor de los dos y es uno de los elementos de la lista. Es mayor o igual que $a$, y por tanto que todos los $y_j$ con $j \le m$, y también que $y_{m+1}$.

### Corrección de `informe`

**Notación.** Sea $\mu$ el parámetro `minimo`, con $\mu \ge 0.02$ (precondición del Teorema 3, necesaria para que `replicasSensibles` termine). Para $g \in \mathrm{Reg}(R)$ sea

$$M_g = \max\{\mathrm{mag}(s) : s \in R_g\}$$

y sea $N(m, \mu) = \lvert \texttt{replicasSensibles}(m, \mu) \rvert$ la cantidad de réplicas esperadas para una magnitud $m$.

**Especificación.**

$$\mathrm{dom}\big(\mathrm{informe}(R,\mu)\big) = \mathrm{Reg}(R)$$

$$\mathrm{informe}(R,\mu)(g) = \mathrm{Resumen}\big(\lvert R_g \rvert,\ M_g,\ N(M_g, \mu)\big) \quad \text{para todo } g \in \mathrm{Reg}(R)$$

**Argumento.** La implementación es

```scala
r.groupBy(_.region).map { case (region, sismos) =>
  val conteo = sismos.size
  val maxima = sismos.map(_.magnitud).max
  val replicas = replicasSensibles(maxima, minimo).size
  region -> Resumen(conteo, maxima, replicas)
}
```

*Paso 1: agrupación y dominio.* Por el Lema 4, `r.groupBy(_.region)` es un mapa $M$ con $\mathrm{dom}(M) = \mathrm{Reg}(R)$ y $M(g) = R_g$. La función del `map` conserva la llave $g$, así que por el Lema 11 el resultado tiene dominio $\mathrm{Reg}(R)$ y, para cada $g$, valor igual a lo que calcula el cuerpo del `case` con `sismos = R_g`.

*Paso 2: conteo.* `conteo = sismos.size = |R_g|`.

*Paso 3: máxima.* `sismos.map(_.magnitud)` es la lista $[\mathrm{mag}(t_1),\dots,\mathrm{mag}(t_m)]$ con $[t_1,\dots,t_m] = R_g$. Todo grupo creado por `groupBy` tiene al menos un elemento (Lema 4: una llave existe solo si algún $s \in R$ la tiene), por lo que $m \ge 1$ y `.max` no se aplica nunca a una lista vacía ni lanza excepción. Por el Lema 12, `maxima = M_g`.

*Paso 4: réplicas.* Por el Teorema 3 del Punto 4, `replicasSensibles(M_g, μ)` es la lista $[r_0,\dots,r_{j-1}]$, donde $r_0 = \mathrm{red}(M_g)$ y $j$ es el menor índice con $r_j \le \mu$. Su tamaño es $j$, así que `replicas = j = N(M_g, μ)`. La precondición $\mu \ge 0.02$ asegura que $j$ existe y que la evaluación termina.

*Paso 5: ensamblado.* Con los Pasos 2 a 4, el valor asociado a $g$ es $\mathrm{Resumen}(\lvert R_g \rvert, M_g, N(M_g,\mu))$, que es la especificación.

**Casos borde.**

- Si $R = []$, el mapa $M$ es vacío, no se ejecuta ningún cuerpo del `case` y el resultado es el mapa vacío.
- Si $M_g \le \mu$, ya $r_0 = \mathrm{red}(M_g) \le \mu$, por lo que $j = 0$ y `replicas = 0`. Esto ocurre, por ejemplo, para una región cuyo sismo más fuerte ya es débil.
- Si $\mu < 0.02$, la secuencia de réplicas se estanca en $0.02$ (observación del Punto 4) y `informe` no termina para ninguna región con $M_g > \mu$. Por eso la precondición es necesaria y no solo conveniente.

**Verificación con el ejemplo del Punto 4.** Para la región Chocó, con $\lvert R_g \rvert = 2$, $M_g = 6.0$ y $\mu = 3.0$, la secuencia es $6.0,\ 4.8,\ 3.84,\ 3.07,\ 2.46$. Aquí $r_3 = 3.07 > 3.0$ y $r_4 = 2.46 \le 3.0$, así que $j = 4$ y

$$\mathrm{informe}(R, 3.0)(\text{Chocó}) = \mathrm{Resumen}(2,\ 6.0,\ 4)$$

que coincide con el valor esperado.

**Consistencia con los puntos anteriores.** Para todo $g \in \mathrm{Reg}(R)$, el primer campo coincide con $\mathrm{conteoPorRegion}(R)(g)$ (Punto 3.2) y el segundo con $\mathrm{magnitudMaxima}(R)(g)$ (Punto 3.3). El tercero es una función solo de $M_g$ y $\mu$ (Punto 4).

### Independencia del orden de recorrido en el Punto 5

Sea $R^\pi$ una permutación cualquiera de $R$.

**Por campo.** Se cumple $\mathrm{Reg}(R^\pi) = \mathrm{Reg}(R)$, así que los dominios coinciden. Para cada $g$:

- El primer campo es $\lvert R_g \rvert$, invariante porque el grupo tiene los mismos sismos (Punto 3).
- El segundo es $M_g$, invariante por el Lema 6.
- El tercero es $N(M_g, \mu)$, una función determinista de $M_g$ y $\mu$ que no consulta el registro, y por tanto también es invariante.

Se concluye que

$$\mathrm{informe}(R^\pi, \mu) = \mathrm{informe}(R, \mu)$$

como mapas.

**Orden de las llaves.** Como en la independencia del orden de recorrido del Punto 3, la igualdad entre mapas no depende del orden de inserción de las llaves, y el orden en que un `Map` las presenta no es parte de la especificación.

**Conclusión.** Igual que `conteoPorRegion` y `magnitudMaxima`, `informe` no depende del orden del registro, porque cada campo es una reducción con un operador asociativo y conmutativo o una función de ese resultado. Esto permitiría calcularlo por partes (por ejemplo, en paralelo, combinando conteo con $+$ y máxima con $\max$ y derivando réplicas al final).