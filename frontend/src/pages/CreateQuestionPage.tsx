import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useMutation } from "@tanstack/react-query";
import { questionsApi, topicsApi } from "@/api/endpoints";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export function CreateQuestionPage() {
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [topicId, setTopicId] = useState("");
  const [difficulty, setDifficulty] = useState("MEDIUM");
  const [tagsStr, setTagsStr] = useState("");
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const { data: topics } = useQuery({
    queryKey: ["topics"],
    queryFn: () => topicsApi.list(),
  });

  const create = useMutation({
    mutationFn: () =>
      questionsApi.create({
        title,
        content,
        topicId,
        difficulty,
        tags: tagsStr.split(",").map((t) => t.trim()).filter(Boolean),
      }),
    onSuccess: (q) => navigate(`/questions/${q.id}`),
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || "Failed to create";
      setError(msg);
    },
  });

  return (
    <div className="container max-w-2xl py-8">
      <Card>
        <CardHeader><CardTitle>Ask a Question</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <Input placeholder="Title" value={title} onChange={(e) => setTitle(e.target.value)} />
          <Textarea placeholder="Content (describe the question in detail)" value={content} onChange={(e) => setContent(e.target.value)} rows={8} />
          <select
            value={topicId}
            onChange={(e) => setTopicId(e.target.value)}
            className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm"
          >
            <option value="">Select topic</option>
            {topics?.map((t) => (
              <option key={t.id} value={t.id}>{t.name}</option>
            ))}
          </select>
          <select
            value={difficulty}
            onChange={(e) => setDifficulty(e.target.value)}
            className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm"
          >
            <option value="EASY">Easy</option>
            <option value="MEDIUM">Medium</option>
            <option value="HARD">Hard</option>
          </select>
          <Input placeholder="Tags (comma-separated)" value={tagsStr} onChange={(e) => setTagsStr(e.target.value)} />
          {error && <p className="text-sm text-destructive">{error}</p>}
          <Button onClick={() => create.mutate()} disabled={create.isPending || !title || !content || !topicId} className="w-full">
            {create.isPending ? "Posting..." : "Post Question"}
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
