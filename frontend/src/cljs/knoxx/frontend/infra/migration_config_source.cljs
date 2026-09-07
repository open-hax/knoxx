(ns knoxx.frontend.infra.migration-config-source
  "Static TypeScript source grammar for Vite and Vitest configuration loading."
  (:require ["typescript" :as ts]
            [knoxx.frontend.law.migration-config :as law]))

(defn- import-facts [^js declaration]
  (let [^js clause (.-importClause declaration)
        ^js bindings (when clause (.-namedBindings clause))
        namespace? (boolean (and bindings (ts/isNamespaceImport bindings)))
        named (when (and bindings (ts/isNamedImports bindings))
                (mapv (fn [^js entry]
                        [(.-text ^js (.-name entry))
                         (.-text ^js (or (.-propertyName entry) (.-name entry)))])
                      (array-seq (.-elements bindings))))
        pairs (concat (when (and clause (.-name clause))
                        [[(.-text ^js (.-name clause)) "default"]])
                      (when namespace? [[(.-text ^js (.-name bindings)) "*"]]) named)
        module (.-text ^js (.-moduleSpecifier declaration))]
    {:module module :clause? (boolean clause)
     :default? (boolean (and clause (.-name clause))) :namespace? namespace?
     :named (mapv second named)
     :bindings (into {} (map (fn [[local exported]] [local {:module module :exported exported}]) pairs))}))

(defn- env-property? [^js node]
  (let [^js env-node (.-expression node)
        ^js process-node (when env-node (.-expression env-node))]
    (and env-node (ts/isPropertyAccessExpression env-node)
         (= "env" (.-text ^js (.-name env-node)))
         process-node (ts/isIdentifier process-node) (= "process" (.-text process-node)))))

(defn- call-facts [bindings ^js node]
  (let [^js callee (.-expression node)
        member? (ts/isPropertyAccessExpression callee)
        ^js target (if member? (.-expression callee) callee)
        arguments (array-seq (.-arguments node))
        ^js base (first arguments)]
    (assoc (when (ts/isIdentifier target) (get bindings (.-text target)))
           :member (when member? (.-text ^js (.-name callee)))
           :argument-count (count arguments)
           :dirname-literals? (boolean (and base (ts/isIdentifier base)
                                            (= "__dirname" (.-text base)) (seq (rest arguments))
                                            (every? ts/isStringLiteral (rest arguments)))))))

(declare passive-expression?)

(defn- passive-object? [bindings locals ^js node]
  (every? (fn [^js property]
            (and (ts/isPropertyAssignment property)
                 (let [property-name (.-name property)]
                   (or (ts/isIdentifier property-name) (ts/isStringLiteral property-name)))
                 (passive-expression? bindings locals (.-initializer property))))
          (array-seq (.-properties node))))

(defn- passive-arrow? [bindings locals ^js node]
  (let [parameters (array-seq (.-parameters node))
        parameter-names (map #(.-text ^js (.-name ^js %)) parameters)]
    (and (every? #(and (ts/isIdentifier (.-name ^js %))
                       (nil? (.-initializer ^js %)) (nil? (.-dotDotDotToken ^js %))) parameters)
         (passive-expression? (apply dissoc bindings parameter-names)
                              (into locals parameter-names)
                              (.-body node)))))

(defn- passive-composite? [bindings locals ^js node]
  (cond
    (ts/isObjectLiteralExpression node) (passive-object? bindings locals node)
    (ts/isArrayLiteralExpression node)
    (every? #(passive-expression? bindings locals %) (array-seq (.-elements node)))
    (ts/isTemplateExpression node)
    (every? #(passive-expression? bindings locals (.-expression ^js %))
            (array-seq (.-templateSpans node)))
    (ts/isArrowFunction node) (passive-arrow? bindings locals node)
    (ts/isParenthesizedExpression node) (passive-expression? bindings locals (.-expression node))
    (ts/isBinaryExpression node)
    (and (contains? #{(.-BarBarToken ts/SyntaxKind) (.-QuestionQuestionToken ts/SyntaxKind)
                     (.-AmpersandAmpersandToken ts/SyntaxKind)} (.-kind ^js (.-operatorToken node)))
         (passive-expression? bindings locals (.-left node))
         (passive-expression? bindings locals (.-right node)))
    :else false))

(defn- passive-expression? [bindings locals ^js node]
  (and node
       (cond
         (or (ts/isStringLiteral node) (ts/isNumericLiteral node)
             (ts/isNoSubstitutionTemplateLiteral node)
             (contains? #{(.-TrueKeyword ts/SyntaxKind) (.-FalseKeyword ts/SyntaxKind)
                          (.-NullKeyword ts/SyntaxKind)} (.-kind node))) true
         (ts/isIdentifier node) (contains? locals (.-text node))
         (ts/isPropertyAccessExpression node) (env-property? node)
         (ts/isCallExpression node)
         (and (law/admitted-call? (call-facts bindings node))
              (every? #(passive-expression? bindings locals %) (array-seq (.-arguments node))))
         :else (passive-composite? bindings locals node))))

(defn- declarations [statements]
  (mapcat #(array-seq (.-declarations ^js (.-declarationList ^js %)))
          (filter ts/isVariableStatement statements)))

(defn- passive-statement? [bindings locals ^js statement]
  (cond
    (or (ts/isImportDeclaration statement) (ts/isEmptyStatement statement)) true
    (ts/isExportAssignment statement)
    (and (not (.-isExportEquals statement))
         (passive-expression? bindings locals (.-expression statement)))
    (ts/isVariableStatement statement)
    (and (pos? (bit-and (.-Const ts/NodeFlags) (.-flags ^js (.-declarationList statement))))
         (every? #(and (ts/isIdentifier (.-name ^js %))
                        (passive-expression? bindings locals (.-initializer ^js %)))
                 (declarations [statement])))
    :else false))

(defn assert-source!
  "Decode config loading syntax and admit only inspected imports and passive expressions."
  [file profile ^js source]
  (let [statements (array-seq (.-statements source))
        imports (mapv import-facts (filter ts/isImportDeclaration statements))
        bindings (apply merge (map :bindings imports))
        locals (into (set (concat ["__dirname" "undefined"] (keys bindings)))
                     (keep #(when (ts/isIdentifier (.-name ^js %)) (.-text ^js (.-name ^js %)))
                           (declarations statements)))]
    (law/assert-source! {:path file :profile profile :imports imports
                         :static-statements? (every? (partial passive-statement? bindings locals) statements)})))
