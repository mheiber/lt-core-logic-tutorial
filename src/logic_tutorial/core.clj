;; A Very Gentle Introduction To Relational & Functional Programming

;; This tutorial will guide you through the magic and fun of combining
;; relational programming (also known as logic programming) with functional
;; programming. This tutorial does not assume that you have any knowledge of Lisp,
;; Clojure, Java, or even functional programming. The only thing this tutorial
;; assumes is that you are not afraid of using the command line and you have used
;; at least one programming language before in your life.


;; What's the point of writing programs in the relational paradigm? First off, aesthetics dammit.

;; Logic programs are simply beautiful as they often have a declarative nature
;; which trumps even the gems found in functional programming languages. Logic
;; programs use search, and thus they are often not muddied up by algorithmic
;; details. If you haven't tried Prolog before, relational programming will at
;; times seem almost magical.

;; However, I admit, the most important reason to learn the relational paradigm is because it's FUN.


;; First Steps
;; ----

;; Ok, we're ready to begin. But first we need to define a namespace and include
;; all core.logic symbols

(ns logic-tutorial.core
  [:refer-clojure :exclude [==]]
  [:use [clojure.core.logic :exclude [appendo]]
        [clojure.core.logic.pldb]
        [clojure.tools.macro :as macro]])

;; You're now working in a place that has the magic of relational programming
;; available to you.

;; Question & Answer
;; ----

;; Unlike most programming systems, with relational programming we can actually ask
;; the computer questions. But before we ask the computer questions, we need define
;; some facts! The first thing we want the computer to know about is that there are
;; men:

(db-rel man x)

;; And then we want to define some men:

(def men
  (db
    [man 'Bob]
    [man 'John]))

;; Now we can ask who are men. Questions are always asked with `run` or `run*`.
;; By convention we'll declare a logic variable `q` and ask the computer to give
;; use the possible values for `q`. Here's an example.

(with-db men
   (run 1 [q] (man q)))

;; We're asking the computer to give us at least one answer to the question - "Who is a man?".
;; We can ask for more than one answer:

(with-db men
  (run 2 [q] (man q)))

;; Now that is pretty cool. What happens if we ask for even more answers?

(with-db men
  (run 3 [q] (man q)))

;; The same result. That's because we’ve only told the computer that two men
;; exist in the world. It can't give results for things it doesn't know about.
;; Let's define another kind of relationship and a fact:

(db-rel fun x)

(def fun-people
  (db
    [fun 'Bob]))

;; Let's ask a new kind of question:

(with-dbs [men fun-people]
  (run* [q]
    (man q)
    (fun q)))

;; There's a couple of new things going on here. We use `run*`. This means we want
;; all the answers the computer can find. The question itself is formulated
;; differently than before because we're asking who is a man *and* is fun. Enter in
;; the following:

(db-rel woman x)

(def facts
  (db
    [woman 'Lucy]
    [woman 'Mary]))

(db-rel likes x y)

;; We have now switched to a more generic name for the database of 'facts', which
;; we will expand with facts about different relations. Relations don't have to be
;; about a single entity. We can define relationship between things!

(def facts
  (-> facts
    (db-fact likes 'Bob 'Mary)
    (db-fact likes 'John 'Lucy)))

(with-dbs [men facts] (run* [q] (likes 'Bob q)))

;; We've added two facts to the 'facts' database and can now ask who likes who!

;; However, let's try this:

(with-dbs [men facts]
  (run* [q]
    (likes 'Mary q)))

;; Hmm that doesn't work. This is because we never actually said *who Mary liked*,
;; only that Bob liked Mary. Try the following:

(def facts
  (db-fact facts likes 'Mary 'Bob))

(with-dbs [men facts]
  (run* [q]
    (fresh [x y]
      (likes x y)
      (== q [x y]))))

;; Wow that's a lot of new information. The fresh expression isn't something we've
;; seen before. Why do we need it? By convention `run` returns single values for
;; `q` which answer the question. In this case we want to know who likes who. This
;; means we need to create logic variables to store these values in. We then assign
;; both these values to `q` by putting them in a Clojure vector (which is like an
;; array in other programming languages).

;; Try the following:

(with-dbs [men facts]
  (run* [q]
    (fresh [x y]
      (likes x y)
      (likes y x)
      (== q [x y]))))

;; Here we only want the list of people who like each other. Note that the order of
;; how we pose our question doesn't matter:

(with-dbs [men facts]
  (run* [q]
    (fresh [x y]
      (likes x y)
      (== q [x y])
      (likes y x))))


;; Some Genealogy
;; --------------

;; Let's define a few more relations.

(db-rel parent x y)
(db-rel male x)
(db-rel female x)

;; We can define relations as functions!

(defn child [x y]
  (parent y x))

;; Huzzah! We can define relations in terms of other relations! Composition to the rescue:

(defn son [x y]
  (all
   (child x y)
   (male x)))

(defn daughter [x y]
  (all
   (child x y)
   (female x)))

(defn grandparent [x y]
  (fresh [z]
    (parent x z)
    (parent z y)))

(defn granddaughter [x y]
  (fresh [z]
    (daughter x z)
    (child z y)))

;; We can now run this query:

(def genealogy
  (db
    [parent 'John 'Bobby]
    [male 'Bobby]))

(with-db genealogy
  (run* [q]
    (son q 'John)))

;; Let's add another fact:

(def genealogy
  (-> genealogy
    (db-fact parent 'George 'John)))

(with-db genealogy
  (run* [q] (grandparent q 'Bobby)))

;; Play around with defining some new facts and using these relations to pose
;; questions about these facts. If you're feeling particularly adventurous, write a
;; new relation and use it.

;; Primitives
;; ----

;; Let's step back for a moment. `core.logic` is built upon a small set of
;; primitives - they are `run`, `fresh`, `==`, and `conde`. We're already pretty
;; familiar with `run`, `fresh`, and `==`. `run` is simple, it let's us `run` our
;; logic programs. `fresh` is also pretty simple, it lets us declare new logic
;; variables. `==` is a bit mysterious and we've never even seen `conde` before.

;; Unification
;; ----

;; Earlier I lied about assignment when using the `==` operator. The `==` operator
;; means that we want to unify two terms. This means we'd like the computer to take
;; two things and attempt to make them equal. If logic variables occur in either of
;; the terms, the computer will try to bind that logic variable to what ever value
;; matches in the other term. If the computer can't make two terms equal, it fails
;; - this is why sometimes we don't see any results.

;; Consider the following:

(run* [q] (== 5 5))

;; Whoa, what does that mean? It means that our question was fine, but that we
;; never actually unified `q` with anything - `_0` just means we have a logic
;; variable that was never bound to a concrete value.

(run* [q] (== 5 4))

;; It's impossible to make 5 and 4 equal to each other, the computer lets us know
;; that no successful answers exist for the question we posed.

(run* [q] (== q 5))

(run* [q]
  (== q 5)
  (== q 4))

;; Once we've unified a logic variable to a concrete value we can unify it again
;; with that value, but we cannot unify with a concrete value that is not equal to
;; what it is currently bound to.

;; Here's an example showing that we can unify complex terms:

(run* [q]
  (fresh [x y]
    (== [x 2] [1 y])
    (== q [x y])))

;; This shows that in order for the two terms `[x 2]` and `[1 y]` to be unified,
;; the logic variable `x` must be bound to 1 and the logic variable `y` must be
;; bound to 2.

;; Note: it's perfectly fine to unify two variables to each other:

(run* [q]
  (fresh [x y]
    (== x y)
    (== q [x y])))

(run* [q]
  (fresh [x y]
    (== x y)
    (== y 1)
    (== q [x y])))

;; Multiple Universes
;; ----

;; By now we're already familiar with conjuction, that is, logical **and**.

(with-dbs [facts fun-people]
  (run* [q]
    (fun q)
    (likes q 'Mary)))

;; We know now to read this as find `q` such that `q` is fun **and** `q` likes Mary.
;; But how to express logical **or**?

(with-dbs [facts fun-people]
  (run* [q]
    (conde
      ((fun q))
      ((likes q 'Mary)))))

;; The above does exactly that - find `q` such that `q` is fun *or* `q` likes Mary.
;; This is the essence of how we get multiple answers from `core.logic`.

;; Magic Tricks
;; ----

;; By now we're tired of genealogy. Let's go back to the cozy world of Computer
;; Science. One of the very first things people introduce in CS are arrays and/or
;; lists. It’s often convenient to take two lists and join them together. In
;; Clojure this functionality exists via `concat`. However we're going to look at a
;; relational version of the function called `appendo`. While `appendo` is
;; certainly slower than `concat` it has magical powers that `concat` does not
;; have.

;; Note: Since core.logic 0.6.3, `appendo` has been included in core.logic itself.

;; Relational functions are written quite differently than their functional
;; counterparts. Instead of return value, we usually make the final parameter be
;; output variable that we'll unify the answer to. This makes it easier to compose
;; relations together. This also means that relational programs in general look
;; quite different from functional programs.

;; Here is the definition for `appendo`.

(defn appendo [l1 l2 o]
  (conde
    ((== l1 ()) (== l2 o))
    ((fresh [a d r]
       (conso a d l1)
       (conso a r o)
       (appendo d l2 r)))))

;; We can pass in logic variables in any one of it's three arguments.
;; Now try the following:

(run* [q] (appendo [1 2] [3 4] q))

;; Seems reasonable. Now try this:

(run* [q] (appendo [1 2] q [1 2 3 4]))

;; Note that `appendo` can infer it's inputs!

;; There’s actually a short hand for writing appendo, we can write it like this.
;; This is pattern matching - it can decrease the amount of boiler plate we have to
;; write for many programs.

;; Zebras
;; ----

;; There's a classic old puzzle sometimes referred to as the Zebra puzzle,
;; sometimes as Einstein's puzzle. Writing an algorithm for solving the constraint
;; is a bit tedious - relational programming allows us to just describe the
;; constraints and it can produce the correct answer for us.

;; The puzzle is described in the following manner.

(defne righto [x y l]
  ([_ _ [x y . ?r]])
  ([_ _ [_ . ?r]] (righto x y ?r)))

(defn nexto [x y l]
  (conde
    ((righto x y l))
    ((righto y x l))))

(defn zebrao [hs]
  (macro/symbol-macrolet [_ (lvar)]
    (all
     (== [_ _ [_ _ 'milk _ _] _ _] hs)
     (firsto hs ['norwegian _ _ _ _])
     (nexto ['norwegian _ _ _ _] [_ _ _ _ 'blue] hs)
     (righto [_ _ _ _ 'ivory] [_ _ _ _ 'green] hs)
     (membero ['englishman _ _ _ 'red] hs)
     (membero [_ 'kools _ _ 'yellow] hs)
     (membero ['spaniard _ _ 'dog _] hs)
     (membero [_ _ 'coffee _ 'green] hs)
     (membero ['ukrainian _ 'tea _ _] hs)
     (membero [_ 'lucky-strikes 'oj _ _] hs)
     (membero ['japanese 'parliaments _ _ _] hs)
     (membero [_ 'oldgolds _ 'snails _] hs)
     (nexto [_ _ _ 'horse _] [_ 'kools _ _ _] hs)
     (nexto [_ _ _ 'fox _] [_ 'chesterfields _ _ _] hs))))

;; That is the entirety of the program. Let's run it:

(run 1 [q] (zebrao q))

;; But how fast is it? Open the Light Table console and evaluate the following:

(dotimes [_ 100] (time (doall (run 1 [q] (zebrao q)))))

;; On my machine, after the JVM has had time to warm up, I see the puzzle can be
;; solved in as little as 3 milliseconds. The Zebra puzzle in and of itself is
;; hardly very interesting. However if such complex constraints can be described
;; and solved so quickly, core.logic is very likely fast enough to be
;; applied to reasoning about types! Only time will tell, but I encourage people to
;; investigate such applications.

;; Next Steps
;; ----

;; Hopefully this short tutorial has revealed some of the beauty of relational
;; programming. To be sure, relational programming as I've presented here has its
;; limitations. Yet, people are actively working on surmounting those limitations
;; in more ways than I really have time to document here.

;; While you can get along just fine as a programmer without using relational
;; programming, many aspects of the tools we use today will seem mysterious without
;; a basic understanding of how relational programming works. It also allows us to
;; add features to our languages that are otherwise harder to implement. For
;; example the elegant type systems (and type inferencing) found in Standard ML and
;; Haskell would be fascinating to model via **core.logic**. I also think that an
;; efficient predicate dispatch system that gives ML pattern matching performance
;; with the open-ended nature of CLOS generic methods would be easily achievable
;; via **core.logic**.

;; Resources
;; ---

;; If you found this tutorial interesting and would like to learn more I recommend
;; the following books to further you understanding of the relational paradigm.

;; * [The Reasoned Schemer](http://mitpress.mit.edu/catalog/item/default.asp?ttype=2&tid=10663)
;; * [Paradigms of Artificial Intelligence Programming](http://norvig.com/paip.html)
;; * [Prolog Programming For Artificial Intelligence](http://www.amazon.com/Prolog-Programming-Artificial-Intelligence-Bratko/dp/0201403757)
;; * [Concepts, Techniques, and Models of Computer Programming](http://www.info.ucl.ac.be/~pvr/book.html)
