import { useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { questionsApi, topicsApi } from "@/api/endpoints";
import { TopicsSidebar } from "@/components/TopicsSidebar";
import { TechStackSidebar } from "@/components/TechStackSidebar";
import { QuestionCard } from "@/components/QuestionCard";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { Flame, ChevronLeft, ChevronRight } from "lucide-react";
import { useState } from "react";

export function HomePage() {
  const [params] = useSearchParams();
  const topicSlug = params.get("topic");
  const [page, setPage] = useState(0);

  // Resolve slug → id
  const { data: topic } = useQuery({
    queryKey: ["topic", topicSlug],
    queryFn: () => topicsApi.bySlug(topicSlug!),
    enabled: !!topicSlug,
  });

  const { data: questions, isLoading } = useQuery({
    queryKey: ["questions", topic?.id, page],
    queryFn: () => questionsApi.list(topic?.id, page, 20),
  });

  const { data: hot } = useQuery({
    queryKey: ["hot"],
    queryFn: () => questionsApi.hot(10),
  });

  return (
    <div className="container py-6 flex gap-6">
      {/* Left sidebar: topics */}
      <TopicsSidebar />

      {/* Center: question feed */}
      <main className="flex-1 min-w-0 space-y-4">
        {/* Hot section */}
        {!topicSlug && hot && hot.length > 0 && (
          <div className="mb-6">
            <h2 className="flex items-center gap-2 text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">
              <Flame className="h-4 w-4 text-orange-500" /> Hot Questions
            </h2>
            <div className="space-y-2">
              {hot.slice(0, 5).map((q) => (
                <QuestionCard key={q.id} q={q} />
              ))}
            </div>
          </div>
        )}

        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">
          {topicSlug ? `${topic?.name || topicSlug} Questions` : "Latest Topics"}
        </h2>

        {isLoading && Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-24 w-full" />
        ))}

        {questions?.content.map((q) => (
          <QuestionCard key={q.id} q={q} />
        ))}

        {questions && questions.totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 pt-4">
            <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
              <ChevronLeft className="h-4 w-4" /> Prev
            </Button>
            <span className="text-sm text-muted-foreground">
              Page {page + 1} of {questions.totalPages}
            </span>
            <Button variant="outline" size="sm" disabled={page >= questions.totalPages - 1} onClick={() => setPage(page + 1)}>
              Next <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        )}

        {questions && questions.content.length === 0 && (
          <p className="text-center text-muted-foreground py-12">No questions yet. Be the first to ask!</p>
        )}
      </main>

      {/* Right sidebar: tech stacks */}
      <TechStackSidebar />
    </div>
  );
}
