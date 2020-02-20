(ns com.fulcrologic.rad.rendering.semantic-ui.blob-field
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom :refer [div input]]
       :clj  [com.fulcrologic.fulcro.dom-server :as dom :refer [div input]])
    #?(:cljs [com.fulcrologic.fulcro.networking.file-upload :as file-upload])
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.attributes :as attr]
    [taoensso.timbre :as log]
    [com.fulcrologic.rad.ui-validation :as validation]
    [com.fulcrologic.rad.blob :as blob]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [com.fulcrologic.rad.rendering.semantic-ui.components :refer [ui-wrapped-dropdown]]
    [com.fulcrologic.rad.rendering.semantic-ui.field :refer [render-field-factory]]))

(defn evt->js-files [evt]
  #?(:cljs
     (let [js-file-list (.. evt -target -files)]
       (map (fn [file-idx]
              (let [js-file (.item js-file-list file-idx)
                    name    (.-name js-file)]
                js-file))
         (range (.-length js-file-list))))))

(defsc FileUploadField [this
                        {::form/keys [form-instance master-form] :as env}
                        {::form/keys [label]
                         ::blob/keys [accept-file-types can-replace?]
                         ::attr/keys [qualified-key] :as attribute}]
  {}
  (let [props              (comp/props form-instance)
        current-url        (get props qualified-key)
        has-current-value? (seq current-url)
        can-replace?       (let [r (?! can-replace?)] (if (boolean? r) r true))
        upload-complete?   false
        valid?             (and upload-complete? has-current-value?)]
    (div :.field {:key (str qualified-key)}
      (dom/label label)
      (dom/input (cond-> {:id       (str qualified-key)
                          :onChange (fn [evt]
                                      (let [file (-> evt evt->js-files first)]
                                        (blob/upload-file! env attribute file)))
                          :type     "file"}
                   accept-file-types (assoc :allow (?! accept-file-types)))))))

(def ui-file-upload-field (comp/computed-factory FileUploadField))

(defn render-file-upload [env attribute]
  (ui-file-upload-field env attribute))