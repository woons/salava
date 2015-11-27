(ns salava.page.edit-test
  (:require [midje.sweet :refer :all]
            [clojure.string :refer [join]]
            [salava.test-utils :refer [test-api-request]]))

(def user-id 1)

(def page-id 1)

(def sample-page-content
  {:name "Test page"
   :description "This is a test description"
   :blocks [{:type "heading" :size "h1" :content "Block title"}
            {:type "badge" :badge_id 1 :format "long"}
            {:type "html" :content "Some content"}
            {:type "file" :files [1]}
            {:type "tag" :tag "Some tag" :format "long" :sort "name"}]})

(facts "about opening a page for editing"
       (let [{:keys [status body]} (test-api-request :get (str "/page/edit/" page-id))]
         (fact "page can be opened for editing and it has valid attributes"
               status => 200
               (keys (:page body)) => (just [:id :user_id :name :description :blocks] :in-any-order))
         (let [user-badges (:body  (test-api-request :get (str "/badge/" user-id)))]
           (fact "user badges are loaded"
                 (map :id (:badges body)) => (just (map :id user-badges) :in-any-order))
           (fact "tags of user badges are loaded"
                 (map :tags (:badges body)) => (just (map :tags user-badges) :in-any-order)))
         (fact "user files are loaded"
               (map :id (:files body)) => (just (map :id (:body (test-api-request :get (str "/file/" user-id)))) :in-any-order)))
       (fact "page does not exist"
             (:status (test-api-request :get (str "/page/edit/99"))) => 500))

(facts "about editing page content"
       (fact "page content can not be saved without a valid page-id"
             (:status (test-api-request :post (str "/page/save_content") sample-page-content)) => 404
             (let [{:keys [body status]} (test-api-request :post (str "/page/save_content/not-integer") sample-page-content)]
               status => 400
               body => "{\"errors\":{\"id\":\"(not (instance? java.lang.Long \\\"not-integer\\\"))\"}}"))
       (fact "page title, desctiption and blocks are required"
             (let [{:keys [body status]} (test-api-request :post (str "/page/save_content/" page-id) {})]
               status => 400
               body => "{\"errors\":{\"name\":\"missing-required-key\",\"description\":\"missing-required-key\",\"blocks\":\"missing-required-key\"}}"))
       (fact "page title must be a non-empty string"
             (let [{:keys [body status]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :name ""))]
               status => 400
               body => (contains "{\"errors\":{\"name\":\"(not")))
       (fact "length of the page title is limited"
             (let [{:keys [body status]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :name (join (repeat 256 "a"))))]
               status => 400
               body => (contains "{\"errors\":{\"name\":\"(not")))
       (fact "block type must be valid"
             (let [{:keys [body status]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "not-valid"}]))]
               status => 400
               body => "{\"errors\":{\"blocks\":[\"(not (matches-some-condition? {:type \\\"not-valid\\\"}))\"]}}")
             (:status (test-api-request :post (str "/page/save_content/" page-id) sample-page-content)) => 200)
       (fact "existing blocks can be editied"
             (let [blocks (-> (test-api-request :get (str "/page/view/" page-id)) :body :blocks)
                   altered-block-content "Altered block title"
                   block-to-edit (-> blocks first (assoc :content altered-block-content) (dissoc :page_id :block_order))]
               (:status (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [block-to-edit]))) => 200
               (let [{:keys [body status]} (test-api-request :get (str "/page/view/" page-id))
                     altered-block (->> body :blocks (filter #(= (:id %) (:id block-to-edit))) first)]
                 status => 200
                 (:content altered-block) => altered-block-content)))
       (fact "saving a heading block requires heading size and content"
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "heading"}]))]
               status => 400
               body =>  "{\"errors\":{\"blocks\":[{\"size\":\"missing-required-key\",\"content\":\"missing-required-key\"}]}}")
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "heading" :size "not-valid" :content ""}]))]
               status => 400
               body =>   "{\"errors\":{\"blocks\":[{\"size\":\"(not (#{\\\"h2\\\" \\\"h1\\\"} \\\"not-valid\\\"))\"}]}}")
             (let [{:keys [status]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "heading" :size "h1" :content "Some title"}]))]
               status => 200)
             (let [{:keys [status]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "heading" :size "h2" :content ""}]))]
               status => 200))
       (fact "saving a badge block requires a badge-id and valid format (if user doesn't own the badge, block won't be saved)"
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "badge"}]))]
               status => 400
               body =>  "{\"errors\":{\"blocks\":[{\"badge_id\":\"missing-required-key\",\"format\":\"missing-required-key\"}]}}")
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "badge" :format "not-valid" :badge_id "not-valid"}]))]
               status => 400
               body =>   "{\"errors\":{\"blocks\":[{\"badge_id\":\"(not (integer? \\\"not-valid\\\"))\",\"format\":\"(not (#{\\\"long\\\" \\\"short\\\"} \\\"not-valid\\\"))\"}]}}")
             (let [{:keys [status]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "badge" :format "long" :badge_id 99}]))]
               status => 200
               (-> (test-api-request :get (str "/page/view/" page-id)) :body :blocks count) => 0)
             (let [{:keys [status]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "badge" :format "long" :badge_id 1}]))]
               status => 200
               (-> (test-api-request :get (str "/page/view/" page-id)) :body :blocks count) => 1))
       (fact "saving a html block requires content"
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "html"}]))]
               status => 400
               body =>  "{\"errors\":{\"blocks\":[{\"content\":\"missing-required-key\"}]}}")
             (let [{:keys [status]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "html" :content ""}]))]
               status => 200)
             (let [{:keys [status]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "html" :content "Some content"}]))]
               status => 200))
       (fact "saving a file block requires collection of file-ids (if user doesn't own all the files, block won't be saved)"
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "file"}]))]
               status => 400
               body =>  "{\"errors\":{\"blocks\":[{\"files\":\"missing-required-key\"}]}}")
             (let [{:keys [status]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "file" :files []}]))]
               status => 200)
             (let [{:keys [status]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "file" :files [1 99]}]))]
               status => 200
               (-> (test-api-request :get (str "/page/view/" page-id)) :body :blocks count) => 0)
             (let [{:keys [status]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "file" :files [1]}]))]
               status => 200
               (-> (test-api-request :get (str "/page/view/" page-id)) :body :blocks count) => 1))
       (fact "saving a badge-tag block requires tag, format and sort parameters"
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "tag"}]))]
               status => 400
               body =>  "{\"errors\":{\"blocks\":[{\"tag\":\"missing-required-key\",\"format\":\"missing-required-key\",\"sort\":\"missing-required-key\"}]}}")
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "tag" :tag 0 :format "not-valid" :sort "not-valid"}]))]
               status => 400
               body => "{\"errors\":{\"blocks\":[{\"tag\":\"(not (instance? java.lang.String 0))\",\"format\":\"(not (#{\\\"long\\\" \\\"short\\\"} \\\"not-valid\\\"))\",\"sort\":\"(not (#{\\\"name\\\" \\\"modified\\\"} \\\"not-valid\\\"))\"}]}}")
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "tag" :tag "" :format "long" :sort "name"}]))]
               status => 400
               body => (contains "{\"errors\":{\"blocks\":[{\"tag\":\"(not"))
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "tag" :tag (join (repeat 256 "a")) :format "long" :sort "name"}]))]
               status => 400
               body => (contains "{\"errors\":{\"blocks\":[{\"tag\":\"(not"))
             (let [{:keys [status]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "tag" :tag "Sample" :format "long" :sort "modified"}]))]
               status => 200)
             (let [{:keys [status]} (test-api-request :post (str "/page/save_content/" page-id) (assoc sample-page-content :blocks [{:type "tag" :tag "Sample" :format "short" :sort "name"}]))]
               status => 200)))