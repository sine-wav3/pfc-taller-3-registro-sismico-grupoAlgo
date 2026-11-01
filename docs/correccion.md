# <strong>Informe de Corección</strong>

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

