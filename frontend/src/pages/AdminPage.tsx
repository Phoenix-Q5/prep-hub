import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { adminApi, type BulkUploadResult, type ValidationResult, type CreateTopicRequest, type DashboardStats, type AdminUser } from "@/api/admin";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";
import { formatRelativeTime } from "@/lib/utils";
import type { Suggestion, PageResponse } from "@/types";
import {
  LayoutDashboard, Upload, MessageSquare, Users, Download,
  FileSpreadsheet, FileJson, FileText, Check, X, AlertCircle,
  ChevronLeft, ChevronRight, Shield, ShieldOff, UserCog,
  CheckCircle2, XCircle, Loader2, Search,
} from "lucide-react";

type Tab = "dashboard" | "upload" | "suggestions" | "users";

export function AdminPage() {
  const [activeTab, setActiveTab] = useState<Tab>("dashboard");

  const tabs: { id: Tab; label: string; icon: React.ReactNode }[] = [
    { id: "dashboard", label: "Dashboard", icon: <LayoutDashboard className="h-4 w-4" /> },
    { id: "upload", label: "Bulk Upload", icon: <Upload className="h-4 w-4" /> },
    { id: "suggestions", label: "Suggestions", icon: <MessageSquare className="h-4 w-4" /> },
    { id: "users", label: "Users", icon: <Users className="h-4 w-4" /> },
  ];

  return (
    <div className="container py-6">
      <div className="flex items-center gap-3 mb-6">
        <Shield className="h-6 w-6 text-primary" />
        <h1 className="text-2xl font-bold">Admin Panel</h1>
      </div>

      {/* Tab navigation */}
      <div className="flex gap-1 border-b mb-6">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
              activeTab === tab.id
                ? "border-primary text-primary"
                : "border-transparent text-muted-foreground hover:text-foreground hover:border-muted"
            }`}
          >
            {tab.icon} {tab.label}
          </button>
        ))}
      </div>

      {/* Tab content */}
      {activeTab === "dashboard" && <DashboardTab />}
      {activeTab === "upload" && <BulkUploadTab />}
      {activeTab === "suggestions" && <SuggestionsTab />}
      {activeTab === "users" && <UsersTab />}
    </div>
  );
}

// ═══════════════════════════════════════════════════════════
// Dashboard Tab
// ═══════════════════════════════════════════════════════════

function DashboardTab() {
  const { data: stats, isLoading } = useQuery({
    queryKey: ["admin", "stats"],
    queryFn: adminApi.stats,
    refetchInterval: 30_000,
  });

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-28" />
        ))}
      </div>
    );
  }

  const cards = [
    { label: "Total Users", value: stats?.totalUsers ?? 0, icon: <Users className="h-5 w-5 text-blue-600" /> },
    { label: "Total Questions", value: stats?.totalQuestions ?? 0, icon: <FileText className="h-5 w-5 text-green-600" /> },
    { label: "Published", value: stats?.publishedQuestions ?? 0, icon: <CheckCircle2 className="h-5 w-5 text-teal-600" /> },
    { label: "Pending Suggestions", value: stats?.pendingSuggestions ?? 0, icon: <MessageSquare className="h-5 w-5 text-amber-600" /> },
  ];

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
      {cards.map((c) => (
        <Card key={c.label}>
          <CardContent className="p-5">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-muted-foreground">{c.label}</span>
              {c.icon}
            </div>
            <p className="text-3xl font-bold">{c.value}</p>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

// ═══════════════════════════════════════════════════════════
// Bulk Upload Tab — with validation, unknown topic resolution, and duplicate detection
// ═══════════════════════════════════════════════════════════

type UploadStep = "select" | "validating" | "review" | "uploading" | "done";

function BulkUploadTab() {
  const [step, setStep] = useState<UploadStep>("select");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [jsonText, setJsonText] = useState("");
  const [uploadMode, setUploadMode] = useState<"file" | "json">("file");
  const [dragActive, setDragActive] = useState(false);
  const [parsedQuestions, setParsedQuestions] = useState<unknown[]>([]);
  const [validation, setValidation] = useState<ValidationResult | null>(null);
  const [result, setResult] = useState<BulkUploadResult | null>(null);
  const [topicForms, setTopicForms] = useState<Record<string, CreateTopicRequest>>({});
  const qc = useQueryClient();

  // ── Parse file to JSON ──────────────────────────────
  const parseFile = async (file: File): Promise<unknown[]> => {
    const text = await file.text();
    const parsed = JSON.parse(text);
    return Array.isArray(parsed) ? parsed : parsed.questions || [];
  };

  // ── Step 1: Validate ────────────────────────────────
  const validateMut = useMutation({
    mutationFn: async () => {
      let questions: unknown[];
      if (uploadMode === "file" && selectedFile) {
        questions = await parseFile(selectedFile);
      } else {
        const parsed = JSON.parse(jsonText);
        questions = Array.isArray(parsed) ? parsed : parsed.questions || [];
      }
      setParsedQuestions(questions);
      return adminApi.validateJson(questions);
    },
    onSuccess: (data) => {
      setValidation(data);
      // Pre-populate topic forms for unknown topics
      const forms: Record<string, CreateTopicRequest> = {};
      data.unknownTopics.forEach((slug) => {
        const name = slug
          .split("-")
          .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
          .join(" ");
        forms[slug] = { name, slug, description: "", colorHex: "#6B7280", featured: false };
      });
      setTopicForms(forms);
      setStep("review");
    },
  });

  // ── Step 2: Create missing topics ───────────────────
  const createTopicsMut = useMutation({
    mutationFn: () => adminApi.batchCreateTopics(Object.values(topicForms)),
    onSuccess: () => {
      // Re-validate to confirm topics are now available
      adminApi.validateJson(parsedQuestions).then((data) => {
        setValidation(data);
        if (data.unknownTopics.length === 0) {
          setTopicForms({});
        }
      });
      qc.invalidateQueries({ queryKey: ["topics"] });
    },
  });

  // ── Step 3: Upload ──────────────────────────────────
  const uploadMut = useMutation({
    mutationFn: () => adminApi.uploadJson(parsedQuestions),
    onSuccess: (data) => {
      setResult(data);
      setStep("done");
      qc.invalidateQueries({ queryKey: ["admin", "stats"] });
      qc.invalidateQueries({ queryKey: ["questions"] });
    },
  });

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(false);
    const file = e.dataTransfer.files[0];
    if (file) { setSelectedFile(file); setStep("select"); setValidation(null); setResult(null); }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) { setSelectedFile(file); setStep("select"); setValidation(null); setResult(null); }
  };

  const reset = () => {
    setStep("select"); setSelectedFile(null); setJsonText(""); setParsedQuestions([]);
    setValidation(null); setResult(null); setTopicForms({});
  };

  const unknownTopicsResolved = validation ? validation.unknownTopics.length === 0 : false;

  return (
    <div className="space-y-6 max-w-3xl">
      {/* Template downloads */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Download Templates</CardTitle>
          <CardDescription>Use these as a starting point for your bulk upload.</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-3">
            <a href="/api/admin/templates/excel" download>
              <Button variant="outline" size="sm">
                <FileSpreadsheet className="h-4 w-4 mr-2 text-green-600" /> Excel (.xlsx)
              </Button>
            </a>
            <a href="/api/admin/templates/csv" download>
              <Button variant="outline" size="sm">
                <FileText className="h-4 w-4 mr-2 text-blue-600" /> CSV (.csv)
              </Button>
            </a>
            <a href="/api/admin/templates/json" download>
              <Button variant="outline" size="sm">
                <FileJson className="h-4 w-4 mr-2 text-amber-600" /> JSON (.json)
              </Button>
            </a>
          </div>
        </CardContent>
      </Card>

      {/* Upload mode toggle */}
      <div className="flex items-center gap-4">
        <div className="flex gap-1 p-1 bg-muted rounded-lg">
          <button onClick={() => { setUploadMode("file"); reset(); }}
            className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors ${uploadMode === "file" ? "bg-background shadow-sm" : "text-muted-foreground"}`}
          >File Upload</button>
          <button onClick={() => { setUploadMode("json"); reset(); }}
            className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors ${uploadMode === "json" ? "bg-background shadow-sm" : "text-muted-foreground"}`}
          >Paste JSON</button>
        </div>
        {step !== "select" && (
          <Button variant="ghost" size="sm" onClick={reset}>Start over</Button>
        )}
      </div>

      {/* ── Step 1: Select file / paste JSON ──────────── */}
      {uploadMode === "file" && (
        <Card>
          <CardContent className="p-6">
            <div
              onDragOver={(e) => { e.preventDefault(); setDragActive(true); }}
              onDragLeave={() => setDragActive(false)}
              onDrop={handleDrop}
              className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${dragActive ? "border-primary bg-primary/5" : "border-muted-foreground/25"}`}
            >
              <Upload className="h-10 w-10 mx-auto mb-3 text-muted-foreground" />
              <p className="text-sm font-medium mb-1">{selectedFile ? selectedFile.name : "Drop file here or click to browse"}</p>
              <p className="text-xs text-muted-foreground mb-3">Supports .json, .csv, .xlsx — max 20MB</p>
              <input type="file" accept=".xlsx,.csv,.json" onChange={handleFileSelect} className="hidden" id="file-upload" />
              <label htmlFor="file-upload"><Button variant="outline" size="sm" asChild><span>Choose File</span></Button></label>
            </div>
            {selectedFile && (
              <div className="flex items-center justify-between mt-4 p-3 bg-muted rounded-md">
                <div className="flex items-center gap-2">
                  <FileSpreadsheet className="h-4 w-4" />
                  <span className="text-sm font-medium">{selectedFile.name}</span>
                  <span className="text-xs text-muted-foreground">({(selectedFile.size / 1024).toFixed(1)} KB)</span>
                </div>
                <Button size="sm" onClick={() => validateMut.mutate()} disabled={validateMut.isPending}>
                  {validateMut.isPending ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : <Search className="h-4 w-4 mr-1" />}
                  {validateMut.isPending ? "Validating..." : "Validate & Preview"}
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {uploadMode === "json" && (
        <Card>
          <CardContent className="p-6 space-y-4">
            <Textarea value={jsonText} onChange={(e) => { setJsonText(e.target.value); setValidation(null); setResult(null); setStep("select"); }}
              placeholder={`[\n  { "title": "...", "content": "...", "topicSlug": "java", ... }\n]`}
              rows={10} className="font-mono text-xs" />
            <Button onClick={() => validateMut.mutate()} disabled={validateMut.isPending || !jsonText.trim()} className="w-full">
              {validateMut.isPending ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : <Search className="h-4 w-4 mr-1" />}
              {validateMut.isPending ? "Validating..." : "Validate & Preview"}
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Error from validation */}
      {validateMut.error && (
        <Card className="border-destructive">
          <CardContent className="p-4">
            <div className="flex items-start gap-2 text-destructive">
              <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
              <div><p className="font-medium">Validation failed</p><p className="text-sm">{(validateMut.error as Error)?.message}</p></div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* ── Step 2: Review validation results ─────────── */}
      {step === "review" && validation && (
        <div className="space-y-4">
          {/* Summary */}
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base">Validation Results</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex flex-wrap gap-4 text-sm">
                <span>Total rows: <strong>{validation.totalRows}</strong></span>
                <span className="text-green-700">Valid: <strong>{validation.validRows}</strong></span>
                {validation.duplicateRows > 0 && (
                  <span className="text-amber-700">Duplicates (will skip): <strong>{validation.duplicateRows}</strong></span>
                )}
                {validation.unknownTopics.length > 0 && (
                  <span className="text-red-700">Unknown topics: <strong>{validation.unknownTopics.length}</strong></span>
                )}
              </div>
            </CardContent>
          </Card>

          {/* Unknown topics — create them */}
          {validation.unknownTopics.length > 0 && (
            <Card className="border-amber-300">
              <CardHeader className="pb-3">
                <CardTitle className="text-base flex items-center gap-2">
                  <AlertCircle className="h-5 w-5 text-amber-600" />
                  Unknown Topics — Create them to proceed
                </CardTitle>
                <CardDescription>
                  These topic slugs were found in your file but don't exist in the database yet. Fill in the details and create them.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                {Object.entries(topicForms).map(([slug, form]) => (
                  <div key={slug} className="flex flex-wrap items-end gap-3 p-3 bg-muted/50 rounded-md">
                    <div className="flex-1 min-w-[150px]">
                      <label className="text-xs text-muted-foreground">Slug</label>
                      <Input value={form.slug} disabled className="bg-muted text-sm" />
                    </div>
                    <div className="flex-1 min-w-[150px]">
                      <label className="text-xs text-muted-foreground">Display Name</label>
                      <Input value={form.name} onChange={(e) => setTopicForms((prev) => ({
                        ...prev, [slug]: { ...prev[slug], name: e.target.value }
                      }))} className="text-sm" />
                    </div>
                    <div className="flex-1 min-w-[200px]">
                      <label className="text-xs text-muted-foreground">Description</label>
                      <Input value={form.description || ""} onChange={(e) => setTopicForms((prev) => ({
                        ...prev, [slug]: { ...prev[slug], description: e.target.value }
                      }))} placeholder="Short description" className="text-sm" />
                    </div>
                    <div className="w-20">
                      <label className="text-xs text-muted-foreground">Color</label>
                      <Input type="color" value={form.colorHex || "#6B7280"} onChange={(e) => setTopicForms((prev) => ({
                        ...prev, [slug]: { ...prev[slug], colorHex: e.target.value }
                      }))} className="h-9 p-1" />
                    </div>
                  </div>
                ))}
                <Button onClick={() => createTopicsMut.mutate()} disabled={createTopicsMut.isPending}>
                  {createTopicsMut.isPending ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : <Check className="h-4 w-4 mr-1" />}
                  Create {Object.keys(topicForms).length} Topic{Object.keys(topicForms).length > 1 ? "s" : ""} & Re-validate
                </Button>
                {createTopicsMut.isSuccess && (
                  <p className="text-sm text-green-700 flex items-center gap-1">
                    <CheckCircle2 className="h-4 w-4" /> Topics created. Re-validating...
                  </p>
                )}
              </CardContent>
            </Card>
          )}

          {/* Duplicate warnings */}
          {validation.duplicateRows > 0 && (
            <Card className="border-blue-200">
              <CardHeader className="pb-3">
                <CardTitle className="text-base flex items-center gap-2">
                  <AlertCircle className="h-5 w-5 text-blue-500" />
                  {validation.duplicateRows} Duplicate{validation.duplicateRows > 1 ? "s" : ""} Found — will be skipped
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="max-h-40 overflow-y-auto space-y-1">
                  {validation.duplicateTitles.map((title, i) => (
                    <p key={i} className="text-sm text-muted-foreground">• {title}</p>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}

          {/* Proceed to upload */}
          <Button
            className="w-full"
            size="lg"
            onClick={() => { setStep("uploading"); uploadMut.mutate(); }}
            disabled={!unknownTopicsResolved || uploadMut.isPending}
          >
            {uploadMut.isPending ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : <Upload className="h-4 w-4 mr-2" />}
            {!unknownTopicsResolved
              ? "Resolve unknown topics first"
              : `Upload ${validation.validRows} question${validation.validRows !== 1 ? "s" : ""}${validation.duplicateRows > 0 ? ` (skipping ${validation.duplicateRows} duplicates)` : ""}`
            }
          </Button>
        </div>
      )}

      {/* Error from upload */}
      {uploadMut.error && (
        <Card className="border-destructive">
          <CardContent className="p-4">
            <div className="flex items-start gap-2 text-destructive">
              <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
              <div><p className="font-medium">Upload failed</p><p className="text-sm">{(uploadMut.error as Error)?.message}</p></div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* ── Step 3: Upload result ─────────────────────── */}
      {step === "done" && result && <UploadResultCard result={result} />}
    </div>
  );
}

function UploadResultCard({ result }: { result: BulkUploadResult }) {
  const isFullSuccess = result.errorCount === 0;

  return (
    <Card className={isFullSuccess ? "border-green-300" : "border-amber-300"}>
      <CardHeader className="pb-3">
        <CardTitle className="text-base flex items-center gap-2">
          {isFullSuccess ? <CheckCircle2 className="h-5 w-5 text-green-600" /> : <AlertCircle className="h-5 w-5 text-amber-600" />}
          Upload {isFullSuccess ? "Complete" : "Completed with Errors"}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex flex-wrap gap-6 text-sm mb-3">
          <span>Total: <strong>{result.totalRows}</strong></span>
          <span className="text-green-700">Imported: <strong>{result.successCount}</strong></span>
          {result.skippedDuplicates > 0 && (
            <span className="text-blue-700">Skipped (duplicates): <strong>{result.skippedDuplicates}</strong></span>
          )}
          {result.errorCount > 0 && (
            <span className="text-red-700">Errors: <strong>{result.errorCount}</strong></span>
          )}
        </div>
        {result.errors.length > 0 && (
          <div className="mt-3 space-y-1 max-h-48 overflow-y-auto">
            {result.errors.map((err, i) => (
              <div key={i} className="flex items-start gap-2 text-sm p-2 bg-red-50 rounded">
                <XCircle className="h-4 w-4 text-red-500 shrink-0 mt-0.5" />
                <div>
                  <span className="text-xs text-muted-foreground">Row {err.row}:</span>{" "}
                  <span className="font-medium">{err.title || "(no title)"}</span>
                  <p className="text-xs text-red-700 mt-0.5">{err.error}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

// ═══════════════════════════════════════════════════════════
// Suggestions Tab
// ═══════════════════════════════════════════════════════════

function SuggestionsTab() {
  const [page, setPage] = useState(0);
  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "suggestions", page],
    queryFn: () => adminApi.pendingSuggestions(page, 20),
  });

  return (
    <div className="space-y-4 max-w-3xl">
      <p className="text-sm text-muted-foreground">
        {data?.totalElements ?? 0} pending suggestions to review
      </p>

      {isLoading && Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-32" />)}

      {data?.content.length === 0 && (
        <Card>
          <CardContent className="p-8 text-center text-muted-foreground">
            <CheckCircle2 className="h-8 w-8 mx-auto mb-2 text-green-500" />
            All caught up — no pending suggestions.
          </CardContent>
        </Card>
      )}

      {data?.content.map((s) => (
        <SuggestionReviewCard
          key={s.id}
          suggestion={s}
          onReviewed={() => {
            qc.invalidateQueries({ queryKey: ["admin", "suggestions"] });
            qc.invalidateQueries({ queryKey: ["admin", "stats"] });
          }}
        />
      ))}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
            <ChevronLeft className="h-4 w-4" /> Prev
          </Button>
          <span className="text-sm text-muted-foreground">
            Page {page + 1} of {data.totalPages}
          </span>
          <Button variant="outline" size="sm" disabled={page >= data.totalPages - 1} onClick={() => setPage(page + 1)}>
            Next <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      )}
    </div>
  );
}

function SuggestionReviewCard({ suggestion: s, onReviewed }: {
  suggestion: Suggestion;
  onReviewed: () => void;
}) {
  const [notes, setNotes] = useState("");
  const [expanded, setExpanded] = useState(false);

  const approve = useMutation({
    mutationFn: () => adminApi.reviewSuggestion(s.id, "APPROVED", notes || undefined),
    onSuccess: onReviewed,
  });

  const reject = useMutation({
    mutationFn: () => adminApi.reviewSuggestion(s.id, "REJECTED", notes || undefined),
    onSuccess: onReviewed,
  });

  const typeLabel: Record<string, string> = {
    NEW_QUESTION: "New Question",
    NEW_TOPIC: "New Topic",
    EDIT_QUESTION: "Edit Question",
    NEW_ANSWER: "New Answer",
  };

  const typeColor: Record<string, string> = {
    NEW_QUESTION: "bg-blue-100 text-blue-800",
    NEW_TOPIC: "bg-purple-100 text-purple-800",
    EDIT_QUESTION: "bg-amber-100 text-amber-800",
    NEW_ANSWER: "bg-green-100 text-green-800",
  };

  return (
    <Card>
      <CardContent className="p-4 space-y-3">
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Badge className={`text-[10px] ${typeColor[s.type] || ""}`}>
                {typeLabel[s.type] || s.type}
              </Badge>
              <span className="text-xs text-muted-foreground">
                by <strong>{s.username}</strong> · {formatRelativeTime(s.createdAt)}
              </span>
            </div>
            {s.rationale && <p className="text-sm mt-1 italic text-muted-foreground">"{s.rationale}"</p>}
          </div>
        </div>

        {/* Payload preview */}
        <button
          onClick={() => setExpanded(!expanded)}
          className="text-xs text-primary hover:underline"
        >
          {expanded ? "Hide details" : "Show payload details"}
        </button>
        {expanded && (
          <pre className="text-xs bg-muted p-3 rounded-md max-h-40 overflow-auto font-mono">
            {JSON.stringify(s.payload, null, 2)}
          </pre>
        )}

        {/* Review controls */}
        <div className="flex items-center gap-2 pt-1 border-t">
          <Input
            placeholder="Review notes (optional)"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            className="flex-1 text-sm"
          />
          <Button
            size="sm"
            onClick={() => approve.mutate()}
            disabled={approve.isPending || reject.isPending}
          >
            {approve.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Check className="h-4 w-4 mr-1" />
            )}
            Approve
          </Button>
          <Button
            size="sm"
            variant="destructive"
            onClick={() => reject.mutate()}
            disabled={approve.isPending || reject.isPending}
          >
            {reject.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <X className="h-4 w-4 mr-1" />
            )}
            Reject
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

// ═══════════════════════════════════════════════════════════
// Users Tab
// ═══════════════════════════════════════════════════════════

function UsersTab() {
  const [page, setPage] = useState(0);
  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "users", page],
    queryFn: () => adminApi.listUsers(page, 50),
  });

  const toggleMut = useMutation({
    mutationFn: (id: string) => adminApi.toggleUser(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin", "users"] }),
  });

  const roleMut = useMutation({
    mutationFn: ({ id, role }: { id: string; role: string }) => adminApi.changeRole(id, role),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin", "users"] }),
  });

  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">{data?.totalElements ?? 0} registered users</p>

      {isLoading && Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-14" />)}

      <div className="border rounded-lg overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-muted/50">
            <tr>
              <th className="text-left px-4 py-2.5 font-medium">User</th>
              <th className="text-left px-4 py-2.5 font-medium">Email</th>
              <th className="text-left px-4 py-2.5 font-medium">Role</th>
              <th className="text-left px-4 py-2.5 font-medium">Status</th>
              <th className="text-left px-4 py-2.5 font-medium">Joined</th>
              <th className="text-right px-4 py-2.5 font-medium">Actions</th>
            </tr>
          </thead>
          <tbody>
            {data?.content.map((u) => (
              <tr key={u.id} className="border-t hover:bg-muted/30 transition-colors">
                <td className="px-4 py-2.5">
                  <div>
                    <p className="font-medium">{u.displayName || u.username}</p>
                    <p className="text-xs text-muted-foreground">@{u.username}</p>
                  </div>
                </td>
                <td className="px-4 py-2.5 text-muted-foreground">{u.email}</td>
                <td className="px-4 py-2.5">
                  <Badge variant={u.role === "ADMIN" ? "default" : "secondary"} className="text-[10px]">
                    {u.role}
                  </Badge>
                </td>
                <td className="px-4 py-2.5">
                  <Badge variant={u.enabled ? "secondary" : "destructive"} className="text-[10px]">
                    {u.enabled ? "Active" : "Disabled"}
                  </Badge>
                </td>
                <td className="px-4 py-2.5 text-muted-foreground text-xs">
                  {formatRelativeTime(u.createdAt)}
                </td>
                <td className="px-4 py-2.5 text-right">
                  <div className="flex items-center justify-end gap-1">
                    <Button
                      variant="ghost"
                      size="sm"
                      title={u.enabled ? "Disable user" : "Enable user"}
                      onClick={() => toggleMut.mutate(u.id)}
                      disabled={toggleMut.isPending}
                    >
                      {u.enabled ? <ShieldOff className="h-4 w-4" /> : <Shield className="h-4 w-4" />}
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      title={u.role === "ADMIN" ? "Demote to USER" : "Promote to ADMIN"}
                      onClick={() =>
                        roleMut.mutate({
                          id: u.id,
                          role: u.role === "ADMIN" ? "USER" : "ADMIN",
                        })
                      }
                      disabled={roleMut.isPending}
                    >
                      <UserCog className="h-4 w-4" />
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
            <ChevronLeft className="h-4 w-4" /> Prev
          </Button>
          <span className="text-sm text-muted-foreground">
            Page {page + 1} of {data.totalPages}
          </span>
          <Button variant="outline" size="sm" disabled={page >= data.totalPages - 1} onClick={() => setPage(page + 1)}>
            Next <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      )}
    </div>
  );
}
