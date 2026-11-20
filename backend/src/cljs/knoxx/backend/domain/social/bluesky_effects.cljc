(ns knoxx.backend.domain.social.bluesky-effects
  "Explicit procedure and record groups from the pinned Lexicon snapshot.
   Classification is separate from permission and payload validation.")

(def procedure-groups
  {:account #{"chat.bsky.actor.deleteAccount"
               "com.atproto.admin.deleteAccount"
               "com.atproto.admin.disableAccountInvites"
               "com.atproto.admin.enableAccountInvites"
               "com.atproto.admin.sendEmail"
               "com.atproto.admin.updateAccountEmail"
               "com.atproto.admin.updateAccountHandle"
               "com.atproto.admin.updateSubjectStatus"
               "com.atproto.identity.refreshIdentity"
               "com.atproto.identity.updateHandle"
               "com.atproto.server.activateAccount"
               "com.atproto.server.confirmEmail"
               "com.atproto.server.createAccount"
               "com.atproto.server.deactivateAccount"
               "com.atproto.server.deleteAccount"
               "com.atproto.server.requestAccountDelete"
               "com.atproto.server.requestEmailConfirmation"
               "com.atproto.server.requestEmailUpdate"
               "com.atproto.server.updateEmail"
               "com.atproto.temp.addReservedHandle"}
   :binary #{"app.bsky.video.abortUpload"
               "app.bsky.video.finishUpload"
               "app.bsky.video.startUpload"
               "app.bsky.video.uploadPart"
               "app.bsky.video.uploadVideo"
               "com.atproto.repo.uploadBlob"}
   :conversation #{"chat.bsky.group.addMembers"
               "chat.bsky.group.approveJoinRequest"
               "chat.bsky.group.createGroup"
               "chat.bsky.group.createJoinLink"
               "chat.bsky.group.disableJoinLink"
               "chat.bsky.group.editGroup"
               "chat.bsky.group.editJoinLink"
               "chat.bsky.group.enableJoinLink"
               "chat.bsky.group.rejectJoinRequest"
               "chat.bsky.group.removeMembers"
               "chat.bsky.group.requestJoin"
               "chat.bsky.group.updateJoinRequestsRead"
               "chat.bsky.group.withdrawJoinRequest"}
   :credential #{"app.bsky.contact.startPhoneVerification"
               "app.bsky.contact.verifyPhone"
               "com.atproto.admin.disableInviteCodes"
               "com.atproto.admin.updateAccountPassword"
               "com.atproto.admin.updateAccountSigningKey"
               "com.atproto.identity.requestPlcOperationSignature"
               "com.atproto.identity.signPlcOperation"
               "com.atproto.identity.submitPlcOperation"
               "com.atproto.server.createAppPassword"
               "com.atproto.server.createInviteCode"
               "com.atproto.server.createInviteCodes"
               "com.atproto.server.createSession"
               "com.atproto.server.deleteSession"
               "com.atproto.server.refreshSession"
               "com.atproto.server.requestPasswordReset"
               "com.atproto.server.reserveSigningKey"
               "com.atproto.server.resetPassword"
               "com.atproto.server.revokeAppPassword"
               "com.atproto.temp.requestPhoneVerification"
               "com.atproto.temp.revokeAccountCredentials"}
   :curate #{"app.bsky.ageassurance.begin"
               "app.bsky.bookmark.createBookmark"
               "app.bsky.bookmark.deleteBookmark"
               "app.bsky.contact.dismissMatch"
               "app.bsky.contact.importContacts"
               "app.bsky.contact.removeData"
               "app.bsky.contact.sendNotification"
               "app.bsky.draft.createDraft"
               "app.bsky.draft.deleteDraft"
               "app.bsky.draft.updateDraft"
               "app.bsky.feed.sendInteractions"
               "app.bsky.graph.muteActor"
               "app.bsky.graph.muteActorList"
               "app.bsky.graph.muteThread"
               "app.bsky.graph.unmuteActor"
               "app.bsky.graph.unmuteActorList"
               "app.bsky.graph.unmuteThread"
               "app.bsky.notification.putActivitySubscription"
               "app.bsky.notification.registerPush"
               "app.bsky.notification.unregisterPush"
               "app.bsky.notification.updateSeen"
               "app.bsky.unspecced.initAgeAssurance"
               "chat.bsky.convo.acceptConvo"
               "chat.bsky.convo.deleteMessageForSelf"
               "chat.bsky.convo.leaveConvo"
               "chat.bsky.convo.lockConvo"
               "chat.bsky.convo.muteConvo"
               "chat.bsky.convo.unlockConvo"
               "chat.bsky.convo.unmuteConvo"
               "chat.bsky.convo.updateAllRead"
               "chat.bsky.convo.updateRead"
               "com.atproto.sync.notifyOfUpdate"
               "com.atproto.sync.requestCrawl"}
   :moderate #{"chat.bsky.moderation.updateActorAccess"
               "com.atproto.moderation.createReport"
               "tools.ozone.communication.createTemplate"
               "tools.ozone.communication.deleteTemplate"
               "tools.ozone.communication.updateTemplate"
               "tools.ozone.inbox.appealActionedSubject"
               "tools.ozone.inbox.putNotificationPreferences"
               "tools.ozone.inbox.updateSeen"
               "tools.ozone.moderation.cancelScheduledActions"
               "tools.ozone.moderation.emitEvent"
               "tools.ozone.moderation.scheduleAction"
               "tools.ozone.queue.assignModerator"
               "tools.ozone.queue.createQueue"
               "tools.ozone.queue.deleteQueue"
               "tools.ozone.queue.routeReports"
               "tools.ozone.queue.unassignModerator"
               "tools.ozone.queue.updateQueue"
               "tools.ozone.report.assignModerator"
               "tools.ozone.report.closeReports"
               "tools.ozone.report.createActivity"
               "tools.ozone.report.reassignQueue"
               "tools.ozone.report.refreshStats"
               "tools.ozone.report.unassignModerator"
               "tools.ozone.safelink.addRule"
               "tools.ozone.safelink.removeRule"
               "tools.ozone.safelink.syncSource"
               "tools.ozone.safelink.updateRule"
               "tools.ozone.set.addValues"
               "tools.ozone.set.deleteSet"
               "tools.ozone.set.deleteValues"
               "tools.ozone.set.upsertSet"
               "tools.ozone.setting.removeOptions"
               "tools.ozone.setting.upsertOption"
               "tools.ozone.team.addMember"
               "tools.ozone.team.deleteMember"
               "tools.ozone.team.updateMember"
               "tools.ozone.verification.grantVerifications"
               "tools.ozone.verification.revokeVerifications"}
   :profile #{"app.bsky.actor.putPreferences"
               "app.bsky.notification.putPreferences"
               "app.bsky.notification.putPreferencesV2"
               "chat.bsky.notification.putPreferences"}
   :publish #{"chat.bsky.convo.sendMessage"
               "chat.bsky.convo.sendMessageBatch"}
   :react #{"chat.bsky.convo.addReaction"
               "chat.bsky.convo.removeReaction"}
   :read #{"tools.ozone.moderation.listScheduledActions"
               "tools.ozone.safelink.queryEvents"
               "tools.ozone.safelink.queryRules"}
   :repo-import #{"com.atproto.repo.importRepo"}
   :repository #{"com.atproto.repo.applyWrites"
               "com.atproto.repo.createRecord"
               "com.atproto.repo.deleteRecord"
               "com.atproto.repo.putRecord"}})

(def procedures
  (into {} (mapcat (fn [[effect ids]] (map #(vector % effect) ids))) procedure-groups))

(def records
  {"app.bsky.actor.contentVisibilityDeclaration" :profile
   "app.bsky.actor.link" :profile
   "app.bsky.actor.profile" :profile
   "app.bsky.actor.status" :profile
   "app.bsky.feed.generator" :curate
   "app.bsky.feed.like" :react
   "app.bsky.feed.post" :publish
   "app.bsky.feed.postgate" :moderate
   "app.bsky.feed.repost" :amplify
   "app.bsky.feed.threadgate" :moderate
   "app.bsky.graph.block" :moderate
   "app.bsky.graph.follow" :curate
   "app.bsky.graph.list" :curate
   "app.bsky.graph.listblock" :moderate
   "app.bsky.graph.listitem" :curate
   "app.bsky.graph.referencelistoptout" :curate
   "app.bsky.graph.starterpack" :curate
   "app.bsky.graph.verification" :moderate
   "app.bsky.labeler.service" :moderate
   "app.bsky.notification.declaration" :profile
   "chat.bsky.actor.declaration" :profile
   "com.atproto.lexicon.schema" :curate})
