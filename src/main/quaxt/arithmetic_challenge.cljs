(ns quaxt.arithmetic-challenge
  (:require
   [clojure.string :as str]
   [reagent.core :as r]
   [clojure.edn :as edn]
   [reagent.dom.client :as rdc]))

;; (defn component []
;;   [:p "Hello world"])

;; ;; React 18+ renders through a "root". Create it once and reuse it, so that
;; ;; hot reloads re-render into the same root instead of creating a new one.

;; (ns ^:figwheel-hooks quaxt.arithmetic-challenge
;;   (:require
;;    [clojure.string :as str]
;;    [reagent.core :as r]
;;    [clojure.edn :as edn]))

(def defaults {:screen :settings
               :selected-setting 0
               :quiz-length 10
               :level 12
               :difficulty {}
               :question-types #{"+" "-" "*" "/"}})

(defonce app-state (r/atom defaults))

(defn all-questions[level question-types]
  (let [max (inc level)]
    (shuffle (concat
              (for [op (disj question-types "-" "/")
                    x (range 1 max)
                    y (range 1 max)]
                {:op op :x x :y y})
              (when (question-types "-")
                (for [y (range 1 max)
                      x (range y (+ y max))]
                  {:op "-" :x x :y y}))
              (when (question-types "/")
                (for [x (range 1 max)
                      y (range 1 max)]
                  {:op "/" :x (* x y) :y y}))))))

(defn sort-by-difficulty[questions]
  (let [{:keys [difficulty]} @app-state]
    (sort (fn[a b] (compare (difficulty b 1000000) (difficulty a 1000000)))
          questions))
  )

(defn make-quiz[n level question-types]
  (shuffle (take n (sort-by-difficulty (all-questions level question-types)))))

(defn now[]
  (.now js/Date))

(defn start-new-quiz[n level question-types]
  (swap! app-state assoc
         :screen :quiz
         :quiz (make-quiz n level question-types)
         :results []
         :user-answer ""
         :start-time (now)))

(def op-to-string
  {"*" "\u00D7"
   "/" "\u00F7"
   "+" "+"
   "-" "\u2212"})

(defn question-to-string[{:keys [op x y]} user-answer]
  (str x " " (op-to-string op) " " y " = " user-answer))

(defn ask[quiz user-answer]
  [:div
   [:div
    {:style {:font-size "13vw"}}
    (question-to-string (first quiz) user-answer)  "_"]])

(defn type-key[text]
  (swap! app-state update :user-answer str text))

(defn key-div[text on-click]
  [:div
   {:style {:height "10vh"}} 
   [:button
    {:style {:height "9vh"
             :font-size "8vh"
             :width "100%"
             :background "#f1f3f4"
             :color "#202124"
             :border "1px solid #f1f3f4"
             :border-radius "4px"
             :cursor "pointer"
             :font-family "arial,sans-serif"
             :text-align "center"}
     :on-click on-click}
    text]])

(defn keypad-key[text]
  [key-div text #(type-key text)])

(defn type-backspace[]
  (swap! app-state
         update
         :user-answer
         (fn[string]
           (subs string 0 (- (count string) 1)))))

(defn backspace-key[]
  [key-div "\u232B" type-backspace])

(defn type-escape[]
  (swap! app-state
         assoc :screen :settings))

(defn as-int[x]
  (if (string? x)
    (js/parseInt x)
    x))

(defn set-local-storage[key-name value]
  (-> (.-localStorage js/window)
      (.setItem key-name value)))

(defn get-local-storage[key-name]
  (-> (.-localStorage js/window)
      (.getItem key-name)))

(def lookup-op
  {"+" +
   "-" -
   "*" *
   "/" /})

(defn right?[{:keys [op x y]} user-answer]
  (= ((lookup-op op) x y) user-answer))

(defn difficulty[{:keys [question time user-answer]}]
  (if (right? question user-answer)
    time
    2000000))

(defn read-state-from-local-storage[]
  (doseq [setting [:difficulty :quiz-length :level :question-types]]
    (let [setting-string (get-local-storage (name setting))
          setting-value (if setting-string
                          (edn/read-string setting-string) (setting defaults))]
      (swap! app-state assoc setting setting-value))))

(defn write-state-to-local-storage[]
  (doseq [setting [:difficulty :quiz-length :level :question-types]]
    (set-local-storage (name setting) (setting @app-state))))

(defn compute-difficulty-table[results]
  (let [old-results (:difficulty @app-state)
        result-map (into {} (map (fn[result]
                                   (difficulty result)
                                   [(:question result)
                                    (difficulty result)]) results))]
    (merge old-results result-map)))

(defn update-difficulty-table[results]
  (let [difficulty-table (compute-difficulty-table results)]
    (swap! app-state assoc :difficulty difficulty-table)))

(defn type-enter[]
  (let [{:keys [difficulty
                level
                quiz
                quiz-length
                results
                screen
                start-time
                user-answer
                question-types]} @app-state]
    (cond (= screen :settings) (start-new-quiz quiz-length level question-types)
          (first quiz) (when-not (= user-answer "")
                         (let [results (conj results 
                                             {:question (first quiz)
                                              :time (- (now) start-time)
                                              :user-answer (as-int user-answer)})]
                           (update-difficulty-table results)
                           (swap! app-state
                                  assoc
                                  :user-answer ""
                                  :quiz (rest quiz)
                                  :results results
                                  :start-time (now))))
          :else (do
                  (write-state-to-local-storage)
                  (start-new-quiz quiz-length level question-types)))))

(defn enter-key[font-size]
  [key-div "\u23CE" type-enter])

(defn escape-key[font-size]
  [key-div "\u238B" type-escape])


(defn keypad[]
  [:table
   {:style {:width "100%" :table-layout " fixed"}}
   [:tbody
    [:tr [:td [keypad-key "7"]][:td [keypad-key "8"]][:td [keypad-key "9"]]]
    [:tr [:td [keypad-key "4"]][:td [keypad-key "5"]][:td [keypad-key "6"]]]
    [:tr [:td [keypad-key "1"]][:td [keypad-key "2"]][:td [keypad-key "3"]]]
    [:tr [:td [keypad-key "0"]]
     [:td [backspace-key]]
     [:td [enter-key]]]]])

(defn right-or-wrong-td[style question user-answer]
  (let [right (right? question user-answer)]
    (let [s (assoc-in style [:style :color] (if right "#00aa00" "#ff0000"))]
      [:td s
       (if right "\u2713" "\u2717")])))

(defn results-total
  "take results and return number of right answers and total time"
  [results]
  (->> results
       (map
        (fn[{:keys [question user-answer time]}]
          [(if (right? question user-answer) 1 0)
           time]))
       (apply map +)))

(defn play-icon[]
  [:svg {:xmlns "http://www.w3.org/2000/svg", :xmlns-xlink "http://www.w3.org/1999/xlink", :width "57", :height "23", :view-box "0 0 96 128"}
   [:path {:fill "green"
           :d "M0 0 l96 64 l-96 64 z"}]])

(defn results-div[]
  (let [{:keys [results quiz-length]}  @app-state
        results (sort
                 (fn[x y] (compare 
                           [(not (right? (:question y) (:user-answer y))) (:time y)]
                           [(not (right? (:question x) (:user-answer x))) (:time x)])) results)
        [right-count total-time] (results-total results)
        style {:style {:font-size "4vh"}}]
    [:table
     [:tbody      
      (map-indexed
       (fn[index
           {:keys [time user-answer]
            {:keys [op x y]} :question}]
         (let [question {:op op :x x :y y}] ^{:key
                                              index}
           [:tr [:td (assoc-in
                      style [:style :text-align] "center") (question-to-string question
                                                                 user-answer)]
            
            (right-or-wrong-td style question user-answer)
            [:td (assoc-in style [:style :text-align] "right") (str  time " ms")]])) results)
      [:tr [:td
            (-> style
                (assoc-in [:style :text-align] "center")
                (assoc-in [:style :border-top] "black 1px solid")
                (assoc-in [:col-span] "2"))
            right-count " / "  quiz-length]
       [:td (-> style
                (assoc-in [:style :text-align] "right")
                (assoc-in [:style :border-top] "black 1px solid")
                (assoc-in [:col-span] "2"))
        total-time " ms"]]
      [:tr
       [:td {:col-span "2"
             :on-click type-enter
             :style {:color "green" :height "7vh" :text-align "center"}} [play-icon]]
       [:td {:col-span "2"} [enter-key "7vh"]]]
      [:tr
       [:td {:col-span "2"
             :on-click type-escape
             :style {:height "7vh" :text-align "center"}} "\u2630"]
       [:td {:col-span "2"} [escape-key "7vh"]]]]]))

(def progress
  (memoize (fn[difficulty level question-types]
             (let [question-color (fn[question]
                                    (let [d (difficulty question 1000000)]
                                      (case d
                                        1000000 "#ffffff"
                                        2000000 "#ff0000"
                                        (let [level (* 0.0512 d)]
                                          (str "rgb(" level ","
                                               level ","
                                               level ")")))))]
               [:svg {:view-box (str "0 0 " (* (count question-types) level) " " level)
                      :width "100%"
                      :height "120"
                      :preserve-aspect-ratio "none"
                      :xmlns "http://www.w3.org/2000/svg"}
                (for [i (range 0 (* (count question-types) level))
                      j (range 0 level)
                      :let [op-index (quot i level)
                            op (nth (filter question-types ["+" "*" "-" "/"]) op-index)
                            [x y] [(inc (mod i level)) (inc j)]
                            [x y] (case op
                                    "+" [x y]
                                    "*" [x y]
                                    "-" [(+ x y) y]
                                    "/" [(* x y) y])
                            question {:op op :x x :y y}]]
                  ^{:key (str/join " " [i j])}
                  [:rect {:x i :y j :width "1" :height "1"
                          :fill (question-color question)}])]))))

(defn quiz-length-icon[]
  (let [right 300]
    [:svg {:xmlns "http://www.w3.org/2000/svg", :xmlns-xlink "http://www.w3.org/1999/xlink", :width "100", :height "50", :view-box "0 0 420 210"}
     [:symbol {:id "question"}
      [:path {:d "M0,40 c 0 -48 64 -48 64 0 q 0 32 -24 32 v 24 h -16 v -40 q 24 0 24 -16 c 0 -24 -32 -24 -32 0 z ", :fill "currentcolor"}]
      [:circle {:cx "32", :cy "120", :r "8", :fill "currentColor"}]]
     [:use {:xlink-href "#question", :x 32, :y "64", :color "black"}]
     [:use {:xlink-href "#question", :x right, :y "56", :color "#666", :stroke "black", :stroke-width "1"}]
     [:use {:xlink-href "#question", :x (+ 8 right), :y "64", :color "#888", :stroke "black", :stroke-width "1"}]
     [:use {:xlink-href "#question", :x (+ 16 right), :y "72", :color "#aaa", :stroke "black", :stroke-width "1"}]
     [:path {:d "M170 160 h64 v-16 z"}]]))

(defn level-icon[]
  [:svg {:xmlns "http://www.w3.org/2000/svg", :xmlns-xlink "http://www.w3.org/1999/xlink", :width "100", :height "50", :view-box "0 0 420 110"}
   [:path {:d "M0 48 h8 v-16 h8 v16 h98 v-16 h8 v16 h8 v8
h-8 v16 h-8 v-16 h-98 v16 h-8 v-16 h-8"}]
   [:path {:d "M170 64 h64 v-16 z"}]

[:path {:d "M280 48 h8 v-16 h8 v16 h4 v-32 h 8 v 32 h 64 v-32 h 8 v32 h4 v-16 h8 v16 h8 v8

h-8 v16 h-8 v-16 h-4 v32 h-8 v-32 h-64 v32 h-8 v-32 h-4 v16 h-8 v-16 h-8 z"}]])

(defn toggle
  "if s contains k remove it, otherwise add it"
  [s k]
  (let [f (if (contains? s k)
            disj
            conj)]
    (f s k)))

(defn toggle-question-type[op]
  (swap! app-state
         update
         :question-types
         (fn[question-types] (toggle question-types op))))

(defn adjust-settings!
  [selected-setting delta]
  (if (> selected-setting 1)
    (toggle-question-type (["+" "*" "-" "/"] (- selected-setting 2)))
    (let [k ([:level :quiz-length] selected-setting)]
      (swap! app-state
             (fn[app-state]
               (-> app-state
                   (update k  (partial + delta)))))))
  (swap! app-state
             (fn[app-state]
               (-> app-state
                   (assoc :selected-setting selected-setting)))))



(defn question-type-toggle-control[op setting-number selected-setting]
  (let [{:keys [question-types]} @app-state]
    [:tr
     (when (= setting-number selected-setting)
       {:style {:border "1px solid black"}})
     [:td {:style {:text-align "center"}} (op-to-string op)]
     [:td {:col-span "3"
           :style {:text-align "center"}}
      [:input
       {:type "checkbox"
        :checked (boolean (question-types op))
        :on-change #((toggle-question-type op)
                     (swap! app-state
                            (fn[app-state]
                              (-> app-state
                                  (assoc :selected-setting setting-number)))))}]]]))

(defn settings-menu[]
  (let [{:keys [level quiz-length selected-setting]} @app-state
        settings-key (fn[selected-setting delta]
                       (let [k ([:level :quiz-length] selected-setting)]
                         [key-div ({1 "+"
                                    -1 "-"} delta)
                          (fn[](adjust-settings! selected-setting delta))]))]
    [:table {:style {:border-collapse "collapse"}}
     [:tbody
      [:tr
       (when (zero? selected-setting) {:style {:border "1px solid black"}})
       [:td [level-icon]] [:td (settings-key 0 -1)] [:td level] [:td (settings-key 0 +1)]]
      [:tr
       (when (= 1 selected-setting) {:style {:border "1px solid black"}})
       [:td [quiz-length-icon]] [:td (settings-key 1 -1)] [:td quiz-length] [:td (settings-key 1 +1)]]
      [question-type-toggle-control "+" 2 selected-setting]
      [question-type-toggle-control "*" 3 selected-setting]
      [question-type-toggle-control "-" 4 selected-setting]
      [question-type-toggle-control "/" 5 selected-setting]
      [:tr
       [:td {:on-click type-enter
             :style {:color "green" :height "7vh" :text-align "center"}} [play-icon]]
       [:td {:col-span "3"}[enter-key "7vh"]]]]]))

(defn quiz-app[]
  (let [{:keys [user-answer quiz difficulty screen level question-types]} @app-state
        question (first quiz)]
    [:div 
     (if (= :settings screen)
       [:div {:style {:margin "auto"
                   :width "fit-content"}} [settings-menu]]
       [:div {:style {:margin "auto"
                   :width "fit-content"}}
        (if question
          [:div {:style {:touch-action "manipulation"}}
           [ask quiz user-answer]
           [keypad]]
          [results-div])])
     [progress difficulty level question-types]]))

;; (defn mount[el]
;;   (r/render-component [quiz-app] el))

;; (defn get-app-element[]
;;   (.getElementById js/document "app"))

;; (defn mount-app-element[]
;;   (mount (get-app-element)))

(defn key-listener[^js/KeyboardEvent key-event]
  (let [{:keys [screen]} @app-state
        settings-screen? (= :settings screen)
        key (.-key key-event)
        change-setting-selection (fn[delta]
                                   (swap! app-state
                                            update
                                            :selected-setting
                                            (fn[selected-setting]
                                              (mod (+ delta selected-setting) 6))))]
    (print "ok1")
    (print  screen)
    (cond
      (#{"0" "1" "2" "3" "4" "5" "6" "7" "8" "9"} key) (type-key key)
      (= key "Backspace") (type-backspace)
      (= key "Enter") (type-enter)
      (= key "Escape") (type-escape)
      (and (= key "ArrowUp") settings-screen?) (change-setting-selection -1)
      (and (= key "ArrowDown") settings-screen?) (change-setting-selection 1)
      (and (#{"ArrowLeft" "-"}  key) settings-screen?) (adjust-settings!  (:selected-setting @app-state) -1)
      (and (#{"ArrowRight" "+"}  key) settings-screen?) (adjust-settings! (:selected-setting @app-state) 1))))

(defn add-key-listener[]
  (.addEventListener js/document "keydown" key-listener))

;; (defn load-listener[x]
;;   (mount-app-element))

;; (defn add-load-listerner[]
;;   (.addEventListener js/window "load" load-listener))

(defonce setup-stuff
  (do
    (read-state-from-local-storage)
    (let [{:keys [quiz-length level question-types]} @app-state]
      (print question-types)
      (add-key-listener)
;      (add-load-listerner)
      (start-new-quiz quiz-length level question-types))
         true))

;; (defn ^:after-load on-reload []
;;   (mount-app-element))

(defonce root (delay (rdc/create-root (.getElementById js/document "app"))))

(defn ^:dev/after-load mount-it []
  (rdc/render @root [quiz-app]))

(defn init []
  (println "Hello World!")
  (mount-it))
