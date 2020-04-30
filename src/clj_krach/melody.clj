(ns clj-krach.melody
  (:require [leipzig.melody :as l]))

(defn update-if [m ks f & args]
  (if (contains? (get-in m (drop-last ks)) (last ks))
    (apply update-in m ks f args)
    m))

(defn format-nr [nr]
  (Double/parseDouble (format "%.3f" (double nr))))

(defn format-options [kvs]
  (apply str (interpose "," (map (fn [[k v]] (str (name k) ": " v)) kvs))))

(defn adsr [a d s r notes]
  (map (fn [note] 
         (let [proportion (/ (:duration note) (+ a d s r))]
           (-> note
               (assoc-in [:opts :attack] (format-nr (* a proportion)))
               (assoc-in [:opts :decay] (format-nr (* d proportion)))
               (assoc-in [:opts :sustain] (format-nr (* s proportion)))
               (assoc-in [:opts :release] (format-nr (* r proportion))))))
       notes))

(defn tempo [timing notes]
  (->> notes
       (l/tempo timing)
       (map (fn [note] 
              (-> note
                  (update-if [:opts :attack] timing)
                  (update-if [:opts :decay] timing)
                  (update-if [:opts :sustain] timing)
                  (update-if [:opts :release] timing))))))

(defn sample [part notes]
  (map #(assoc % :type :sample :part part) notes))

(defn opts [key value notes] 
  (map #(assoc-in % [:opts key] value) notes))

(defn amp [amp-value notes]
  (map #(assoc-in % [:opts :amp] amp-value) notes))

(defn pan [pan-value notes]
  (map #(assoc-in % [:opts :pan] pan-value) notes))