export interface ReportSubmissionResponse {
  serviceInstanceId: number;
  nextReporterLinkToken: string | null;
  nextReporterLinkFollowUpToken: string | null;
  nextReporterLinkActiveDate: string | null;
}
