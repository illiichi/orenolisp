俺のlisp
====
This is a structural clojure editor for producing sound using Overtone(https://github.com/overtone/overtone), which is a wrapper library for sound engine, SuperCollider.

see:
https://youtu.be/RuU0HI-paik

# How to launch:
Dependencies:
- A java runtime that supports class file version 54.0 or greater (Java 10 or OpenJDK 11 supports this).
- Leiningen.
- Supercollider IDE

1. launch SuperCollider IDE and evaluate below SC codes:
  - `s.options.maxLogins = 10;`
  - `s.boot;`
2. In your terminal, run `lein repl`.
3. Then paste this: `(load-file "src/orenolisp/launch.clj")`

# Features

## Sparse keymap which depends on context

This editor has two mode, typing-mode and selecting-mode.
By switching to selecting-mode, you can move around elements and manipulate expressions without modifier keys .
For example, type `j`/`k` to move to the next/previous element, respectively(This idea is inspired by Emacs plugin lispy-mode: https://github.com/abo-abo/lispy).
![](https://raw.githubusercontent.com/illiichi/orenolisp/images/desc_move.gif)

Keymap will be determined depending on the context (what you are selecting). For example:

- When you are selecting something containing elements (such as parentheses or vector)
    - `a` - jump to first element
    - `s` - jump to second element
    - `e` - jump to last element
    - `l` - jump to parent element
- When you are selecting number
    - `a` or `z` - increment/decrement first digit
    - `s` or `x` - increment/decrement second digit
    - `d` or `c` - multiply by 2 or 1/2
    - `D` or `C` - multiply by 10 or 1/10
![](https://raw.githubusercontent.com/illiichi/orenolisp/images/desc_num.gif)

By typing `Alt-n`, you can turn current function into another function in the same category like:.
  - Sin Wave -> Saw Wave -> Pulse Wave
  - White noise -> Pink Noise -> ...
  - Low-Pass Filter -> High-Pass Filter -> ...

If you are selecting a parentheses, the first element will be changed.
![](https://raw.githubusercontent.com/illiichi/orenolisp/images/desc_next.gif)

## Non-text representation to suit the domain
This editor uses graphics to represent the parentheses. The table below show the equivalent codes of text and of this editor. Due to graphical representation of parentheses which is expanded vertically, it's easier to realize the correspondence of each parentheses. Brankets are represented using squared enclojures to distinguish the kind of parethesis clearly.

<table><tbody><tr><td>
<pre>
(let [freq 500
      func (fn [ratio c-freq]
             (-> (sin-osc (* freq ratio))
                 (* (sin-osc c-freq)
                    (lf-noise1 1/4))))]
  (-> (map func
           [1 3/2 5/4 11/7]
           (reductions * 100 [3/2 4/5]))
      splay))
</pre>
</td>
<td>
<img src="https://raw.githubusercontent.com/illiichi/orenolisp/images/desc_paren.png" />
</td>
</tr></tbody></table>

This editor also replaces some functions with their graphical representation. The Overtone expression below and its image on the editor generate sine wave with varying frequencies from 100 to 1000 in 16 secs:
<table><tbody><tr><td>
<pre>
(sin-osc (line 100 1000 16))
</pre>
</td><td>
<img src="https://raw.githubusercontent.com/illiichi/orenolisp/images/desc_gauge.png" />
</td></tr></tbody></table>

The background color indicates how much it has been processed. The expression will convert into final value automatically after completion.
![](https://raw.githubusercontent.com/illiichi/orenolisp/images/desc_gauge.gif)

## Transformation
The transformation of variable binding is supported by the following instructions:
1. mark an expression to be bound
2. move to the parent expression to be wrapped by `let`
3. call transformation command
4. type a variable name
You can call transformation command again inside `let` expression to define another variable.
![](https://raw.githubusercontent.com/illiichi/orenolisp/images/desc_let.gif)

Writing code for transformation in the structural editor is much simpler than in the text editor because there is no need to parse the text. The code below is the transformation of variable-binding.
```
(defn- make-let-binding [editor]
  (let [parent-editor (-> '(let [___ ___])
                          conv/convert-sexp->editor
                          (ed/move :root))
        second-placeholder-id (ed/get-id parent-editor [:child :right :child :right])]
    (some-> editor
            (ed/add-editor :parent parent-editor)
            (ed/with-marks (fn [editor [marked-node-id]]
                             (-> editor
                                 (ed/jump marked-node-id)
                                 (ed/swap second-placeholder-id)
                                 (ed/move :left)))))))
```
`___` in the code represents a placeholder to be filled simultaneously after the transformation. This code works as below.

1. Marking the expression to be bound and selecting the expression to be wrapped by `let` by the user
![](https://raw.githubusercontent.com/illiichi/orenolisp/images/desc_let-bind.png)

2. Injecting the expression `(let [___ ___])` as parent into selected parentheses
```
(-> (let [___ ___]
      (* (sin-osc 1000) (env-gen (env-perc 0 0.125) (impulse 8))))
    (ringz 10 0.001) tanh)
```

3. Swaping the second placeholder and the marked expression
```
(-> (let [___ 1000]
      (* (sin-osc ___) (env-gen (env-perc 0 0.125) (impulse 8))))
    (ringz 10 0.001) tanh)
```

4. Finally the placeholders will be filled by the user
![](https://raw.githubusercontent.com/illiichi/orenolisp/images/desc_multicursor.png)


You can also transform the expression into an anonymous function and wrap it by `map`
The transformation with multiple arguments is available through multiple marks. You can call the transformation command again inside `map` expression to append another argument.
![](https://raw.githubusercontent.com/illiichi/orenolisp/images/desc_map.gif)
