(ns salava.admin.ui.admintool
  (:require [reagent.core :refer [atom]]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for current-path]]
            [salava.core.i18n :refer [t]]
            [salava.admin.ui.helper :refer [checker admin?]]
            [salava.admin.ui.admintool-content :refer [admin-modal]]))


(defn open-admintool-modal
  ([]
   (let [{:keys [item-type item-id]} (checker (current-path))
         state (atom {:mail          {:subject ""
                                      :message ""}
                      :visible_area  ""
                      :item_type     item-type
                      :item_id       item-id
                      :item_owner    ""
                      :item_owner_id nil
                      :image_file    nil
                      :name          ""
                      :info          {:created     "20.2.2010"
                                      :description "joo jee"}
                      :gallery-state nil
                      :init-data nil})]
     (ajax/GET
      (path-for (str "/obpv1/admin/"item-type"/" item-id))
      {:handler (fn [data]
                  (swap! state assoc :name (:name data)
                         :image_file (:image_file data)
                         :item_owner (:item_owner data)
                         :item_owner_id (:item_owner_id data)
                                                  :info (:info data))
                  (m/modal! [admin-modal state nil nil] {:size :lg}))})))
  ([item-type item-id gallery-state init-data]
   (let [ state (atom {:mail          {:subject ""
                                       :message ""}
                       :visible_area  ""
                       :item_type     item-type
                       :item_id       item-id
                       :item_owner    ""
                       :item_owner_id nil
                       :image_file    nil
                       :name          ""
                       :info          {:created     "20.2.2010"
                                       :description "joo jee"}
                       :gallery-state gallery-state
                       :init-data init-data})]
     (ajax/GET
      (path-for (str "/obpv1/admin/"item-type"/" item-id))
      {:handler (fn [data]
                  (swap! state assoc :name (:name data)
                         :image_file (:image_file data)
                         :item_owner (:item_owner data)
                         :item_owner_id (:item_owner_id data)
                                                  :info (:info data))
                  (m/modal! [admin-modal state] {:size :lg}))}))))

(defn admintool []
  (if (admin?)
    [:div
     [m/modal-window]
     [:a {:class    "bottom-link pull-right"
          :on-click #(do (.preventDefault %)
                         (open-admintool-modal))}
      [:i {:class "fa fa-lock"}] "admin tools"]]))

(defn admin-gallery-badge [item-id item-type state init-data]
  (if (admin?)
    [:div
     [m/modal-window]
     [:a {:class    "bottom-link pull-right"
          :on-click #(do (.preventDefault %)
                         (open-admintool-modal "badges" item-id state init-data))}
      [:i {:class "fa fa-lock"}] "admin tools"]]))

(defn admintool-gallery-page [item-id item-type state init-data user-id]
  (if (admin?)
    [:div
     [m/modal-window]
     [:a {:class    "bottom-link pull-right"
          :on-click #(do (.preventDefault %)
                         (open-admintool-modal "page" item-id state init-data))}
      [:i {:class "fa fa-lock"}] "admin tools"]]))



