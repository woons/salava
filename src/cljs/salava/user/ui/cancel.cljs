(ns salava.user.ui.cancel
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [input-valid? navigate-to path-for]]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.user.schemas :as schemas]))

(defn password-valid? [password]
  (input-valid? (:password schemas/User) password))

(defn cancel-account [state]
  (ajax/POST
    (path-for "/obpv1/user/delete")
    {:params  {:password (:password @state)}
     :handler (fn [data]
                (if (= (:status data) "error")
                  (swap! state assoc :error-message (t :user/Erroroccuredduringaccountcancellation))
                  (navigate-to "/user/login")))}))

(defn cancel-form [state]
  (let [password-atom (cursor state [:password])]
    [:div {:class "form-horizontal"}
     (if (:error-message @state)
       [:div {:class "alert alert-warning" :role "alert"}
        (:error-message @state)])
     [:div.form-group
      [:label {:class "col-xs-3"
               :for "input-password"}
       (t :user/Password)
       [:span.form-required " *"]]
      [:div.col-xs-9
       [:input {:class       "form-control"
                :id          "input-password"
                :type        "password"
                :name        "password"
                :read-only   true
                :on-change   #(reset! password-atom (.-target.value %))
                :on-focus    #(.removeAttribute (.-target %) "readonly")
                :placeholder (t :user/Tocancelaccountenterpassword)
                :value       @password-atom}]]]
     [:button {:class    "btn btn-warning"
               :disabled (if-not (password-valid? @password-atom)
                           "disabled")
               :on-click #(cancel-account state)}
      (t :user/Cancelaccount)]]))

(defn content [state]
  [:div {:id "cancel-account"}
   [:h1.uppercase-header (t :user/Cancelaccount)]
   [:div {:id "cancel-info"}
    [:p (t :user/Cancelaccountinstructions1)]
    [:p (t :user/Cancelaccountinstructions2) ":"]
    [:p (t :user/Cancelaccountinstructions3) ":"]
    [:ul
     [:li (t :user/Goto) " " [:a {:href (path-for "/badge/export")} (t :user/Badgeexport)] " " (t :user/page)]
     [:li {:dangerouslySetInnerHTML
         {:__html (t :user/Cancelaccountinstructions4)}}]]
    [:p (t :user/Cancelaccountinstructions5) ":"]
    [:ul
     [:li (t :user/Goto) " " [:a {:href (path-for "/badge/export")} (t :user/Badgeexport)] " " (t :user/page)]
     [:li {:dangerouslySetInnerHTML
         {:__html (t :user/Cancelaccountinstructions6)}}]
     [:li {:dangerouslySetInnerHTML
         {:__html (t :user/Cancelaccountinstructions7)}}]
     [:li  (t :user/Cancelaccountinstructions8)]]
    [:p [:b (t :user/Cancelaccountinstructions9)]]]
   [:div {:class "panel"}
    [:div.panel-body
     (cancel-form state)]]])

(defn init-data []
  (ajax/GET
    (path-for "/obpv1/user/test")
    {:handler (fn [])}))

(defn handler [site-navi]
  (let [state (atom {:password "" :error-message nil})]
    (init-data)
    (fn []
      (layout/default site-navi (content state)))))
