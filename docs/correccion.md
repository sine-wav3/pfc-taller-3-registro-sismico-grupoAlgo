# Informe de corrección: Taller 3, registro sísmico

Este informe argumenta, con notación matemática, que las funciones de los Puntos 1 y 2 cumplen su especificación. Para cada una se enuncia la especificación, se demuestra que la implementación la satisface y se estudia si el resultado depende del orden en que se recorre el registro.

La estructura es la siguiente: primero se fija la notación y se definen formalmente las operaciones sobre listas; luego se demuestran unos lemas generales; finalmente se aplican a cada función.

---

## 0. Notación

Una lista finita se escribe $L = [x_1, x_2, \dots, x_m]$ con $m \ge 0$. La lista vacía es $[\,]$, el operador $x :: L$ agrega $x$ al inicio de $L$ y $L_1 \mathbin{+\!\!+} L_2$ es la concatenación.

El registro es $R = [s_1, \dots, s_n]$ con $n \ge 0$. De cada sismo $s$ interesan dos atributos:

- $\mathrm{reg}(s)$: la región del sismo.
- $\mathrm{mag}(s) \in \mathbb{R}$: la magnitud del sismo.

Definiciones auxiliares:

- $\mathrm{Reg}(R) = \{\, \mathrm{reg}(s) : s \in R \,\}$ es el conjunto de regiones presentes en $R$.
- $R_g = [\, s \in R : \mathrm{reg}(s) = g \,]$ es la sublista de los sismos de la región $g$, conservando el orden de $R$.
- $\mathrm{prefijo}_t(R) = [s_1,\dots,s_t]$ es el prefijo de los primeros $t$ elementos.

### 0.1. Semántica de las operaciones usadas

Se toman como definición las ecuaciones por casos de cada operación sobre listas:

$$\mathrm{map}(f)([\,]) = [\,] \qquad \mathrm{map}(f)(x :: L) = f(x) :: \mathrm{map}(f)(L)$$

$$\mathrm{filt}(p)([\,]) = [\,] \qquad \mathrm{filt}(p)(x :: L) = \begin{cases} x :: \mathrm{filt}(p)(L) & \text{si } p(x) \\ \mathrm{filt}(p)(L) & \text{si no } \end{cases}$$

$$\mathrm{flatMap}(f)([\,]) = [\,] \qquad \mathrm{flatMap}(f)(x :: L) = f(x) \mathbin{+\!\!+} \mathrm{flatMap}(f)(L)$$

La operación `withFilter` de Scala produce el mismo resultado que $\mathrm{filt}$ cuando va seguida de `map` o `flatMap`, y la desazucaración de `for` se traduce según:

$$\texttt{for \{ x <- L; if p(x) \} yield f(x)} \;=\; \mathrm{map}(f)\big(\mathrm{filt}(p)(L)\big)$$

$$\texttt{for \{ x <- A; y <- B; if q(x,y) \} yield h(x,y)} \;=\; \mathrm{flatMap}\big(x \mapsto \mathrm{map}(y \mapsto h(x,y))(\mathrm{filt}(y \mapsto q(x,y))(B))\big)(A)$$

La notación de comprensión de listas que se usa en las especificaciones es:

$$[\, f(x) \mid x \leftarrow L,\ p(x) \,] \;=\; \mathrm{map}(f)\big(\mathrm{filt}(p)(L)\big)$$

es decir, la lista de las imágenes por $f$ de los elementos de $L$ que cumplen $p$, en el orden de $L$.

---

## 1. Lemas generales

### Lema 1 (guardas encadenadas)

Para toda lista $L$ y predicados $p$, $q$:

$$\mathrm{filt}(q)\big(\mathrm{filt}(p)(L)\big) = \mathrm{filt}(x \mapsto p(x) \wedge q(x))(L)$$

**Demostración.** Por inducción sobre la longitud de $L$.

- Caso base, $L = [\,]$: ambos lados son $[\,]$.
- Paso inductivo, $L = x :: L'$, suponiendo el resultado para $L'$:
    - Si $p(x)$ es falso, $\mathrm{filt}(p)(L) = \mathrm{filt}(p)(L')$, y por hipótesis de inducción el lado izquierdo es $\mathrm{filt}(p \wedge q)(L')$. El lado derecho también es $\mathrm{filt}(p \wedge q)(L')$, porque $p(x) \wedge q(x)$ es falso.
    - Si $p(x)$ es verdadero, $\mathrm{filt}(p)(L) = x :: \mathrm{filt}(p)(L')$. Si $q(x)$ es verdadero, el lado izquierdo es $x :: \mathrm{filt}(q)(\mathrm{filt}(p)(L'))$, que por hipótesis es $x :: \mathrm{filt}(p \wedge q)(L')$, igual al lado derecho. Si $q(x)$ es falso, el lado izquierdo es $\mathrm{filt}(q)(\mathrm{filt}(p)(L'))$, igual a $\mathrm{filt}(p \wedge q)(L')$, que es el lado derecho.

Queda demostrado el lema.

### Lema 2 (`for` con una guarda)

Para toda lista $L$, predicado $p$ y función $f$, la expresión `for { x <- L; if p(x) } yield f(x)` es la lista

$$[\, f(x_i) : 1 \le i \le m,\ p(x_i) \,]$$

con $[x_1,\dots,x_m] = L$. En palabras: contiene $f(x_i)$ exactamente para los $x_i$ que cumplen $p$, una vez por cada uno, en el mismo orden que en $L$.

**Demostración.** Por la desazucaración, la expresión es $\mathrm{map}(f)(\mathrm{filt}(p)(L))$. Se razona por inducción sobre $m$.

- Caso base, $m = 0$: $\mathrm{filt}(p)([\,]) = [\,]$ y $\mathrm{map}(f)([\,]) = [\,]$. La lista de la derecha, indexada sobre un rango vacío, también es vacía.
- Paso inductivo, $L = x_1 :: L'$ con $L' = [x_2,\dots,x_m]$, suponiendo el resultado para $L'$:
    - Si $p(x_1)$ es verdadero, $\mathrm{map}(f)(\mathrm{filt}(p)(L)) = f(x_1) :: \mathrm{map}(f)(\mathrm{filt}(p)(L'))$. Por hipótesis de inducción, el resto es la lista de las imágenes de los elementos de $L'$ que cumplen $p$, así que el total es la lista deseada, que empieza por $f(x_1)$.
    - Si $p(x_1)$ es falso, el resultado es $\mathrm{map}(f)(\mathrm{filt}(p)(L'))$, que por hipótesis es la lista deseada, y $x_1$ no aporta nada, como corresponde.

Queda demostrado el lema.

### Lema 3 (`for` con dos generadores y una guarda)

Para listas $A = [a_1,\dots,a_p]$ y $B = [b_1,\dots,b_q]$, un predicado $c(x,y)$ y una función $h$, la expresión

`for { x <- A; y <- B; if c(x,y) } yield h(x,y)`

es la concatenación, en el orden de $a_1, a_2, \dots, a_p$, de las listas

$$\big[\, h(a_i, b_j) : 1 \le j \le q,\ c(a_i, b_j) \,\big]$$

Es decir, los elementos salen ordenados primero por la posición $i$ en $A$ y, para igual $i$, por la posición $j$ en $B$ (orden lexicográfico de $(i,j)$), y aparece exactamente un elemento por cada par $(i,j)$ que cumple $c$.

**Demostración.** Por la desazucaración, la expresión es $\mathrm{flatMap}(g)(A)$ con $g(x) = \mathrm{map}(y \mapsto h(x,y))(\mathrm{filt}(y \mapsto c(x,y))(B))$. Por el Lema 2, $g(a_i)$ es exactamente la lista $[\, h(a_i, b_j) : 1 \le j \le q,\ c(a_i,b_j) \,]$. Por la ecuación de $\mathrm{flatMap}$, aplicada por inducción sobre $p$, se tiene $\mathrm{flatMap}(g)(A) = g(a_1) \mathbin{+\!\!+} g(a_2) \mathbin{+\!\!+} \cdots \mathbin{+\!\!+} g(a_p)$, que es la concatenación enunciada. Queda demostrado el lema.

### Lema 4 (`groupBy` conserva los elementos por llave)

Sea $k$ una función de llave. Para toda lista $R$, el resultado de `R.groupBy(k)` es un mapa $M$ tal que:

1. El dominio de $M$ es $\{\, k(s) : s \in R \,\}$.
2. Para toda llave $g$ del dominio, $M(g) = [\, s \in R : k(s) = g \,]$, es decir, los elementos de $R$ con llave $g$, en el orden en que aparecen en $R$.

**Demostración.** Se razona sobre el estado del mapa $M_t$ después de procesar los primeros $t$ elementos de $R$, por inducción sobre $t$. La hipótesis es que $M_t$ cumple 1 y 2 respecto del prefijo de longitud $t$.

- Caso base, $t = 0$: el mapa está vacío, y el prefijo vacío no tiene llaves ni elementos.
- Paso inductivo: al procesar $s_{t+1}$ con llave $g' = k(s_{t+1})$, el mapa $M_{t+1}$ se obtiene de $M_t$ agregando $s_{t+1}$ **al final** de la lista de $g'$ (creándola si no existía) y dejando las demás llaves iguales. Entonces:
    - El dominio de $M_{t+1}$ es el de $M_t$ unido con $\{g'\}$, que es el conjunto de llaves del prefijo de longitud $t+1$.
    - Para $g \ne g'$, $M_{t+1}(g) = M_t(g)$, que por hipótesis son los elementos del prefijo de llave $g$. Como $s_{t+1}$ no tiene llave $g$, siguen siendo los del nuevo prefijo.
    - Para $g'$, $M_{t+1}(g') = M_t(g') \mathbin{+\!\!+} [s_{t+1}]$, es decir, los elementos anteriores de llave $g'$ en su orden, seguidos de $s_{t+1}$, que es el último elemento del nuevo prefijo. Luego conserva el orden.

Con $t = n$ se obtiene el enunciado. Queda demostrado el lema.

### Lema 5 (`groupMapReduce`)

Sean $k$ una función de llave, $v$ una función de valor y $\oplus$ un operador binario. Para toda lista $R$, el resultado de `R.groupMapReduce(k)(v)(⊕)` es un mapa $A$ tal que:

1. El dominio de $A$ es $\{\, k(s) : s \in R \,\}$.
2. Para toda llave $g$ del dominio, sea $[t_1,\dots,t_m] = [\, s \in R : k(s) = g \,]$ con $m \ge 1$. Entonces
   $$A(g) = \big(\cdots\big((v(t_1) \oplus v(t_2)) \oplus v(t_3)\big) \cdots \oplus v(t_m)\big)$$
   Si además $\oplus$ es asociativo, esto es $v(t_1) \oplus v(t_2) \oplus \cdots \oplus v(t_m)$ sin importar los paréntesis.

**Demostración.** Se razona sobre el estado $A_t$ después de procesar los primeros $t$ elementos, por inducción sobre $t$.

- Caso base, $t = 0$: el mapa está vacío.
- Paso inductivo: al procesar $s_{t+1}$ con llave $g' = k(s_{t+1})$:
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

---

## 2. Punto 1

### 2.1. Corrección de `fuertes`

**Especificación.** Dado un umbral $u \in \mathbb{R}$, `fuertes` debe devolver las regiones de los sismos cuya magnitud supera $u$, en el orden en que aparecen en el registro y sin quitar repetidos:

$$\mathrm{fuertes}(R,u) = [\, \mathrm{reg}(s_i) : 1 \le i \le n,\ \mathrm{mag}(s_i) > u \,]$$

**Argumento.** La implementación es

```scala
for { s <- r; if s.magnitud > umbral } yield s.region
```

que es una expresión del tipo del Lema 2 con $L = R$, $p(s) = (\mathrm{mag}(s) > u)$ y $f = \mathrm{reg}$. Por el Lema 2, el resultado contiene $\mathrm{reg}(s_i)$ exactamente para los $s_i$ con $\mathrm{mag}(s_i) > u$, uno por cada sismo que cumple y en el orden de $R$. No interviene ninguna operación que elimine repetidos, así que si dos sismos que cumplen tienen la misma región, esa región aparece dos veces. Esto coincide con la especificación.

**Casos borde.** Si $R = [\,]$, el resultado es $[\,]$ (caso base del Lema 2). Si $u$ es mayor o igual que toda magnitud, ningún elemento cumple $p$ y el resultado es $[\,]$. La desigualdad es estricta, por lo que un sismo con magnitud exactamente $u$ no se incluye. Queda demostrada la corrección.

### 2.2. Corrección de `enRegion`

**Especificación.** Dada una región $g$:

$$\mathrm{enRegion}(R,g) = [\, \mathrm{mag}(s_i) : 1 \le i \le n,\ \mathrm{reg}(s_i) = g \,]$$

**Argumento.** La implementación es

```scala
for { s <- r; if s.region == region } yield s.magnitud
```

que es el Lema 2 con $p(s) = (\mathrm{reg}(s) = g)$ y $f = \mathrm{mag}$. Por el Lema 2 el resultado es exactamente la lista de la especificación. Equivale a $\mathrm{map}(\mathrm{mag})(R_g)$, es decir, las magnitudes de los sismos de la región $g$ en el orden del registro.

**Casos borde.** Si $g \notin \mathrm{Reg}(R)$, ningún sismo cumple $p$ y el resultado es $[\,]$, que es lo que pide el enunciado para una región ausente, sin lanzar excepción. Si $R = [\,]$ el resultado es $[\,]$. Queda demostrada la corrección.

### 2.3. Corrección de `paresCercanos`

**Definiciones.** Sea $\delta \ge 0$ el parámetro `dif`. Para dos regiones $a$ y $b$ se define el predicado de cercanía:

$$C(a,b) \iff \exists\, s, s' \in R :\ \mathrm{reg}(s) = a \,\wedge\, \mathrm{reg}(s') = b \,\wedge\, \lvert \mathrm{mag}(s) - \mathrm{mag}(s') \rvert \le \delta$$

Sea $G = [g_0, g_1, \dots, g_{k-1}]$ la lista de las regiones distintas de $R$ en orden de primera aparición. Es el resultado de `r.map(_.region).distinct`: `distinct` conserva la primera aparición de cada elemento y elimina las siguientes, de modo que los $g_i$ son todos distintos entre sí y $\{g_0,\dots,g_{k-1}\} = \mathrm{Reg}(R)$.

**Especificación.** El resultado debe contener las parejas de regiones distintas con algún sismo de magnitudes a lo sumo $\delta$ de distancia, cada pareja una sola vez sin importar el orden, y con las regiones en el orden en que aparecen en el registro:

$$\mathrm{paresCercanos}(R,\delta) = \big[\, (g_i, g_j) \;:\; 0 \le i < j < k,\ C(g_i,g_j) \,\big]$$

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

*Paso 3: aplicación del Lema 3.* Por el Lema 3, el resultado es la concatenación, para $i = 0,1,\dots,k-1$ en ese orden, de las listas $[\,(g_i,g_j) : 0 \le j < k,\ i < j,\ C(g_i,g_j)\,]$, con $j$ creciente dentro de cada una. Esto es exactamente la lista de la especificación, en orden lexicográfico de $(i,j)$.

*Paso 4: regiones distintas.* Como los $g_i$ son todos distintos, la condición $i < j$ implica $g_i \ne g_j$. Luego toda pareja producida es de regiones distintas.

*Paso 5: cada pareja sale una sola vez.* Cada pareja de índices $(i,j)$ aparece a lo sumo una vez, por el Lema 3. Dos parejas de índices distintas $(i,j) \ne (i',j')$ con $i<j$ e $i'<j'$ dan parejas de regiones distintas como pares ordenados, y tampoco coinciden como pares no ordenados: de $\{g_i,g_j\} = \{g_{i'},g_{j'}\}$ y la condición $i<j$, $i'<j'$ se sigue $(i,j) = (i',j')$, porque los $g$ son todos distintos. Por tanto, si una pareja salió como $(g_i, g_j)$, no vuelve a salir como $(g_j, g_i)$, que tendría $j > i$ en la primera posición y no pasaría la guarda.

*Paso 6: no se pierde ninguna pareja válida.* El predicado $C$ es simétrico:

$$C(a,b) \iff C(b,a)$$

En efecto, si $s, s'$ son los testigos de $C(a,b)$, entonces $s', s$ son los testigos de $C(b,a)$, porque $\lvert x - y \rvert = \lvert y - x \rvert$. Entonces, para toda pareja no ordenada $\{g_i, g_j\}$ con $i \ne j$ y $C(g_i,g_j)$, la orientación con el menor índice primero también cumple $C$, y se incluye en el resultado.

*Paso 7: orden de las regiones en cada pareja.* Cada pareja produce primero la región de menor índice, es decir, la que aparece antes en el registro. Así, si $\texttt{"Valle"}$ aparece antes que $\texttt{"Choco"}$, la pareja es $(\texttt{"Valle"}, \texttt{"Choco"})$ y nunca $(\texttt{"Choco"}, \texttt{"Valle"})$.

**Casos borde.** Si $R = [\,]$, entonces $G = [\,]$, $Z = [\,]$ y el resultado es $[\,]$. Si $R$ tiene una sola región, $k = 1$ y no existen índices con $i < j$, luego el resultado es $[\,]$.

**Verificación con el ejemplo del enunciado.** Con $G = [\text{Valle}, \text{Santander}, \text{Choco}]$ y las menores diferencias entre magnitudes de regiones distintas, $0.2$ para (Valle, Santander), $0.7$ para (Valle, Choco) y $0.9$ para (Santander, Choco):

- Con $\delta = 0.6$ solo cumple $C(\text{Valle},\text{Santander})$.
- Con $\delta = 1.0$ cumplen las tres, en el orden lexicográfico $(0,1)$, $(0,2)$, $(1,2)$.
- Con $\delta = 0.1$ no cumple ninguna y el resultado es $[\,]$.

**Observación sobre punto flotante.** La comparación $\le \delta$ se hace con números `Double`. Cuando la diferencia exacta en decimal es igual a $\delta$, el error de representación puede alterar el resultado de la comparación. La demostración anterior es válida sobre los reales; sobre `Double` es válida salvo en esos casos frontera. Las pruebas deben elegir valores que no dependan de la igualdad exacta.

Queda demostrada la corrección.

### 2.4. Independencia del orden de recorrido en el Punto 1

Sea $R^\pi$ el registro obtenido al aplicar una permutación $\pi$ a $R$.

**`fuertes` y `enRegion`.** Por el Lema 2, cada elemento de $R$ aporta su imagen al resultado si y solo si cumple el predicado, y esa decisión depende solo del elemento mismo, no de su posición ni de sus vecinos. Por tanto, el *multiconjunto* de elementos del resultado es el mismo para $R$ y para $R^\pi$. Lo único que cambia al permutar es el orden de la lista resultante, que sigue el orden del registro. Esto no es un defecto: el enunciado especifica "en el orden en que aparecen", por lo que el orden de la salida es parte de la especificación y se deriva del orden de la entrada de forma determinista.

**`paresCercanos`.** El predicado $C(a,b)$ está definido con un cuantificador existencial sobre $R$, y un $\exists$ no depende del orden en que se examinan los elementos. Por tanto, el *conjunto de parejas no ordenadas* $\{\{a,b\} : a \ne b,\ C(a,b)\}$ es el mismo para $R$ y para $R^\pi$. Lo que sí cambia al permutar es la lista $G$, y con ella el orden de listado y la orientación de cada pareja. Ambos están determinados por el orden de primera aparición, que la especificación define como criterio.

---

## 3. Punto 2

### 3.1. Corrección de `porRegion`

**Especificación.**

$$\mathrm{dom}\big(\mathrm{porRegion}(R)\big) = \mathrm{Reg}(R), \qquad \mathrm{porRegion}(R)(g) = R_g \ \text{ para todo } g \in \mathrm{Reg}(R)$$

**Argumento.** La implementación es `r.groupBy(_.region)`. Por el Lema 4 con $k = \mathrm{reg}$, el dominio del mapa es $\{\mathrm{reg}(s) : s \in R\} = \mathrm{Reg}(R)$ y la lista asociada a cada región $g$ contiene los sismos de $R$ con región $g$, en el orden del registro, que es $R_g$. Una región que no está en el registro no es llave del mapa, lo que cumple la condición del enunciado de que las regiones ausentes no aparecen. Si $R = [\,]$ el mapa es vacío. Queda demostrada la corrección.

### 3.2. Corrección de `conteoPorRegion`

**Especificación.**

$$\mathrm{conteoPorRegion}(R)(g) = \lvert R_g \rvert \ \text{ para todo } g \in \mathrm{Reg}(R), \qquad \mathrm{dom} = \mathrm{Reg}(R)$$

**Argumento.** La implementación es `r.groupMapReduce(_.region)(_ => 1)(_ + _)`. Se aplica el Lema 5 con $k = \mathrm{reg}$, $v(s) = 1$ y $\oplus = +$. La suma de enteros es asociativa. Para $g \in \mathrm{Reg}(R)$ con $[t_1,\dots,t_m] = R_g$:

$$\mathrm{conteoPorRegion}(R)(g) = \underbrace{1 + 1 + \cdots + 1}_{m \text{ sumandos}} = m = \lvert R_g \rvert$$

El dominio es $\mathrm{Reg}(R)$ por el Lema 5, por lo que las regiones ausentes no aparecen, y si $R = [\,]$ el resultado es el mapa vacío.

**Consistencia con `porRegion`.** Para toda $g \in \mathrm{Reg}(R)$ se cumple $\mathrm{conteoPorRegion}(R)(g) = \lvert \mathrm{porRegion}(R)(g) \rvert$, pues ambos valores son $\lvert R_g \rvert$. Además, la suma de todos los conteos es $\sum_{g \in \mathrm{Reg}(R)} \lvert R_g \rvert = n$, porque los conjuntos $R_g$ forman una partición de los elementos de $R$.

**Sobre el desbordamiento de `Int`.** La suma de `Int` en la JVM es aritmética módulo $2^{32}$, que sigue siendo asociativa y conmutativa. Además, ningún conteo supera $n$, que es el tamaño de una lista en memoria, por lo que en la práctica no hay desbordamiento. Queda demostrada la corrección.

### 3.3. Corrección de `magnitudMaxima`

**Especificación.**

$$\mathrm{magnitudMaxima}(R)(g) = \max\{\, \mathrm{mag}(s) : s \in R_g \,\} \ \text{ para todo } g \in \mathrm{Reg}(R), \qquad \mathrm{dom} = \mathrm{Reg}(R)$$

**Argumento.** La implementación es `r.groupMapReduce(_.region)(_.magnitud)(math.max)`. Se aplica el Lema 5 con $k = \mathrm{reg}$, $v = \mathrm{mag}$ y $\oplus = \max$. El operador $\max$ es asociativo:

$$\max(a, \max(b,c)) = \max(\max(a,b), c)$$

pues ambos lados son el mayor de los tres valores. Por el Lema 5, para $[t_1,\dots,t_m] = R_g$:

$$\mathrm{magnitudMaxima}(R)(g) = \max\big(\mathrm{mag}(t_1), \dots, \mathrm{mag}(t_m)\big) = \max\{\mathrm{mag}(s) : s \in R_g\}$$

Como $m \ge 1$ en todo grupo, el máximo existe y no se lanza excepción por reducir una colección vacía. Si $R = [\,]$, el resultado es el mapa vacío.

**Exactitud numérica.** $\max(x,y)$ devuelve uno de sus dos argumentos sin modificarlo, por lo que el valor entregado es exactamente una de las magnitudes del registro y no acumula error de redondeo. (Se supone que ninguna magnitud es `NaN`, caso que el modelo de datos del taller no contempla.) Queda demostrada la corrección.

### 3.4. Independencia del orden de recorrido en el Punto 2

Sea $R^\pi$ una permutación cualquiera de $R$.

**`conteoPorRegion` y `magnitudMaxima`.** Por el Lema 5, el valor de cada llave es una reducción de los valores del grupo. Los operadores $+$ sobre los enteros y $\max$ sobre los reales son asociativos y conmutativos. Como el grupo de la región $g$ en $R^\pi$ contiene los mismos sismos que en $R$, pero posiblemente en otro orden, el Lema 6 asegura que la reducción da el mismo valor. Por tanto:

$$\mathrm{conteoPorRegion}(R^\pi) = \mathrm{conteoPorRegion}(R), \qquad \mathrm{magnitudMaxima}(R^\pi) = \mathrm{magnitudMaxima}(R)$$

Esta propiedad es más fuerte que en el Punto 1: el resultado completo no depende ni del orden del registro ni de la forma de parentizar la reducción. Es la propiedad que haría posible calcular los grupos por partes, incluso en paralelo, y combinar los resultados parciales.

**`porRegion`.** Por el Lema 4, la lista asociada a $g$ contiene los sismos de la región $g$ en el orden en que aparecen. Al permutar $R$, el *multiconjunto* de sismos de cada región es el mismo, y lo único que cambia es el orden interno de cada lista, que por especificación sigue el orden de la entrada. Los dominios de ambos mapas coinciden, pues dependen solo de $\mathrm{Reg}(R)$.

**Orden de las llaves.** La igualdad entre mapas no depende del orden de inserción de las llaves, y el orden en que un `Map` las presenta no forma parte de la especificación. Por eso los resultados de `conteoPorRegion` y `magnitudMaxima` son iguales como mapas para cualquier permutación del registro.


---

## Punto 4: corrección de `replicas` y `replicasSensibles`

Sea $r_0 = \text{red}(m)$ y $r_{k+1} = \text{red}(0.8 \cdot r_k)$, con
$\text{red}(x) = \text{round}(100x)/100$. Esta es la especificación.

La secuencia es infinita y perezosa, así que la propiedad se enuncia sobre los
primeros $n$ elementos, no sobre toda la secuencia.

**Teorema 1.** $\forall k \in \mathbb{N} : \texttt{replicas}(m)(k) = r_k$.

*Caso base* $k = 0$: la cabeza de `iterate` es $\text{red}(m) = r_0$.

*Paso* : por hipótesis, el elemento $k$ es $r_k$. `iterate` define el
siguiente como $f(r_k) = \text{red}(0.8 \cdot r_k) = r_{k+1}$. 

**Teorema 2 (monotonía).** $r_{k+1} \le r_k$ para todo $k$.
Como $r_k \ge 0$ es múltiplo de $0.01$, $\text{red}(r_k) = r_k$. Como
$0.8\, r_k \le r_k$ y $\text{red}$ es monótona,
$r_{k+1} = \text{red}(0.8\, r_k) \le \text{red}(r_k) = r_k$.

**Teorema 3.** Si $\text{min} \ge 0.02$, entonces
$\texttt{replicasSensibles}(m, \text{min}) = [r_0, \dots, r_{j-1}]$, donde $j$
es el menor índice con $r_j \le \text{min}$.

*Prueba.* Por el Teorema 2, si $r_j \le \text{min}$ entonces
$r_i \le \text{min}$ para todo $i \ge j$, y por minimalidad de $j$,
$r_i > \text{min}$ para $i < j$. `takeWhile` toma exactamente los $r_i$ con
$i < j$ y se detiene en $r_j$.

*Terminación.* Si $r_k \ge 0.03$, entonces $0.2\, r_k > 0.005$, así que el
redondeo no recupera la diferencia y $r_{k+1} \le r_k - 0.01$. La secuencia
baja al menos $0.01$ por paso hasta llegar a $\le 0.02 \le \text{min}$, luego
$j$ existe. 

**Observación.** Si $\text{min} < 0.02$, la secuencia se estanca en $0.02$
($\text{red}(0.016) = 0.02$) y `replicasSensibles` no termina. Es una
limitación del redondeo a dos decimales que pide el enunciado.

**Verificación con el informe.** Para $m = 6.0$, $\text{min} = 3.0$:
$r = 6.0,\ 4.8,\ 3.84,\ 3.07,\ 2.46$, así que $j = 4$ y la lista tiene 4
elementos. Coincide con `Resumen(2, 6.0, 4)` de Chocó.

