(ns salava.gallery.ui.badge-view
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.gallery.ui.badge-content :refer [badge-content]]
            [salava.core.ui.share :refer [share-buttons]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]))

(defn content [state]
  (let [{content :content badge-content-id :badge-content-id} @state
        {{name :name} :badge} content]
    [:div {:id "badge-gallery-view"}
     [:div.panel
      [:div.panel-body
       [share-buttons (str (session/get :site-url) "/gallery/badgeview/" badge-content-id) name true true (cursor state [:show-link-or-embed])]
       [badge-content content]]]]))

(defn init-data [state badge-content-id]
  (ajax/GET
    (str "/obpv1/gallery/public_badge_content/" badge-content-id)
    {:handler (fn [data] (swap! state assoc :content data))}))

(defn handler [site-navi params]
  (let [badge-content-id (:badge-content-id params)
        state (atom {:badge-content-id badge-content-id
                     :content nil
                     :show-link-or-embed nil})]
    (init-data state badge-content-id)
    (fn []
      (if (session/get :user)
        (layout/default site-navi (content state))
        (layout/landing-page (content state))))))