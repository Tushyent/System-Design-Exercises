import React, { useState, useEffect } from 'react';
import { api } from '../api/client';
import { 
  Book, 
  BookCopy, 
  CopyDetail, 
  CrawlerJob, 
  Member, 
  MemberProfile360, 
  Loan, 
  Reservation, 
  Fine, 
  AuditLog, 
  SystemConfig, 
  Role, 
  CopyStatus, 
  LoanStatus, 
  ReservationStatus, 
  FineStatus 
} from '../types';
import { 
  SlidersHorizontal, 
  BookPlus, 
  CopyPlus, 
  Globe, 
  Play, 
  RotateCw, 
  CheckCircle2, 
  AlertCircle, 
  Clock, 
  Database,
  Barcode,
  Layers,
  Sparkles,
  Users,
  UserCheck,
  UserX,
  UserPlus,
  Shield,
  ShieldAlert,
  KeyRound,
  Search,
  Filter,
  Check,
  X,
  CreditCard,
  DollarSign,
  AlertTriangle,
  History,
  BookOpen,
  ArrowRight,
  Settings,
  Archive,
  RefreshCw,
  FileText,
  Edit2,
  Trash2
} from 'lucide-react';

export const AdminPage: React.FC = () => {
  // Current user info
  const storedUser = localStorage.getItem('user');
  const currentUser = storedUser ? JSON.parse(storedUser) : null;
  const isAdmin = currentUser?.role === 'ADMIN';

  // Active Tab
  type TabType = 'circulation' | 'members' | 'inventory' | 'loans' | 'reservations' | 'fines' | 'crawler' | 'audit' | 'staff' | 'config';
  const [activeTab, setActiveTab] = useState<TabType>('circulation');

  // Shared Notifications
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // -------------------------------------------------------------
  // TAB 1: CIRCULATION DESK (CHECKOUT & RETURN)
  // -------------------------------------------------------------
  const [checkoutMemberQuery, setCheckoutMemberQuery] = useState('');
  const [checkoutBarcode, setCheckoutBarcode] = useState('');
  const [checkoutLoanDays, setCheckoutLoanDays] = useState(14);
  const [checkoutOverride, setCheckoutOverride] = useState(false);
  const [checkoutOverrideReason, setCheckoutOverrideReason] = useState('');
  const [checkedOutReceipt, setCheckedOutReceipt] = useState<any | null>(null);
  const [memberEligibility, setMemberEligibility] = useState<any | null>(null);
  const [checkingEligibility, setCheckingEligibility] = useState(false);

  const [returnBarcode, setReturnBarcode] = useState('');
  const [returnCondition, setReturnCondition] = useState('NORMAL');
  const [returnDamageFee, setReturnDamageFee] = useState<number>(0);
  const [returnNotes, setReturnNotes] = useState('');
  const [returnReceipt, setReturnReceipt] = useState<any | null>(null);

  const handleVerifyMember = async () => {
    if (!checkoutMemberQuery.trim()) return;
    setCheckingEligibility(true);
    setMemberEligibility(null);
    try {
      const eligibility = await api.members.checkEligibility(checkoutMemberQuery.trim());
      setMemberEligibility(eligibility);
    } catch (err: any) {
      setErrorMsg(err.message || 'Member not found or ineligibility could not be verified');
    } finally {
      setCheckingEligibility(false);
    }
  };

  const handleStaffCheckout = async (e: React.FormEvent) => {
    e.preventDefault();
    setSuccessMsg(null);
    setErrorMsg(null);
    setLoading(true);
    try {
      const res = await api.circulation.staffCheckout({
        memberIdentifier: checkoutMemberQuery.trim(),
        barcode: checkoutBarcode.trim(),
        loanDays: checkoutLoanDays,
        overrideLimit: checkoutOverride,
        overrideReason: checkoutOverrideReason,
      });
      setCheckedOutReceipt(res);
      setSuccessMsg(`Checkout successful! Book "${res.bookTitle}" issued to member.`);
      setCheckoutBarcode('');
      handleVerifyMember();
    } catch (err: any) {
      setErrorMsg(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleStaffReturn = async (e: React.FormEvent) => {
    e.preventDefault();
    setSuccessMsg(null);
    setErrorMsg(null);
    setLoading(true);
    try {
      const res = await api.circulation.staffReturn({
        barcode: returnBarcode.trim(),
        condition: returnCondition,
        damageFee: returnCondition !== 'NORMAL' ? returnDamageFee : 0,
        staffNotes: returnNotes,
      });
      setReturnReceipt(res);
      setSuccessMsg(`Return processed for barcode ${returnBarcode}. Condition: ${returnCondition}.`);
      setReturnBarcode('');
      setReturnNotes('');
      setReturnCondition('NORMAL');
      setReturnDamageFee(0);
    } catch (err: any) {
      setErrorMsg(err.message);
    } finally {
      setLoading(false);
    }
  };

  // -------------------------------------------------------------
  // TAB 2: MEMBER MANAGEMENT
  // -------------------------------------------------------------
  const [membersList, setMembersList] = useState<Member[]>([]);
  const [memberSearch, setMemberSearch] = useState('');
  const [memberStatusFilter, setMemberStatusFilter] = useState('');
  const [selected360Member, setSelected360Member] = useState<MemberProfile360 | null>(null);
  const [loading360, setLoading360] = useState(false);
  const [showRegisterMemberModal, setShowRegisterMemberModal] = useState(false);

  // Edit Member Modal State
  const [editingMember, setEditingMember] = useState<Member | null>(null);
  const [editMemberName, setEditMemberName] = useState('');
  const [editMemberEmail, setEditMemberEmail] = useState('');
  const [editMemberDept, setEditMemberDept] = useState('');
  const [editMemberPhone, setEditMemberPhone] = useState('');
  const [editMemberLimit, setEditMemberLimit] = useState(5);

  // Suspend Member Modal State
  const [suspendingMember, setSuspendingMember] = useState<Member | null>(null);
  const [suspensionReasonInput, setSuspensionReasonInput] = useState('Unreturned library assets / overdue dues');
  const [suspensionUntilDate, setSuspensionUntilDate] = useState('2026-11-30');

  // New Member Form
  const [regName, setRegName] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPassword, setRegPassword] = useState('Welcome123!');
  const [regDepartment, setRegDepartment] = useState('Computer Science');
  const [regPhone, setRegPhone] = useState('');
  const [regLimit, setRegLimit] = useState(5);

  const [newPasswordValue, setNewPasswordValue] = useState('');

  const fetchMembers = async () => {
    try {
      const data = await api.members.getAll(memberSearch, memberStatusFilter);
      setMembersList(data || []);
    } catch (err: any) {
      console.error(err);
    }
  };

  const handleView360 = async (id: string) => {
    setLoading360(true);
    try {
      const profile = await api.members.getProfile360(id);
      setSelected360Member(profile);
    } catch (err: any) {
      setErrorMsg(err.message);
    } finally {
      setLoading360(false);
    }
  };

  const handleOpenEditMember = (member: Member) => {
    setEditingMember(member);
    setEditMemberName(member.name || '');
    setEditMemberEmail(member.email || '');
    setEditMemberDept(member.department || '');
    setEditMemberPhone(member.phone || '');
    setEditMemberLimit(member.maxBorrowLimit || 5);
  };

  const handleSaveMemberEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingMember) return;
    try {
      await api.members.update(editingMember.id, {
        name: editMemberName,
        email: editMemberEmail,
        department: editMemberDept,
        phone: editMemberPhone,
        maxBorrowLimit: editMemberLimit,
      });
      setSuccessMsg(`Member details updated for ${editMemberName}.`);
      setEditingMember(null);
      fetchMembers();
      if (selected360Member && selected360Member.member.id === editingMember.id) {
        handleView360(editingMember.id);
      }
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  const handleOpenSuspendModal = (member: Member) => {
    setSuspendingMember(member);
    setSuspensionReasonInput('Unreturned library assets / overdue dues');
    // Default 30 days out
    const d = new Date();
    d.setDate(d.getDate() + 30);
    setSuspensionUntilDate(d.toISOString().split('T')[0]);
  };

  const handleConfirmSuspension = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!suspendingMember) return;
    try {
      const untilIso = suspensionUntilDate ? new Date(suspensionUntilDate).toISOString() : undefined;
      await api.members.updateStatus(suspendingMember.id, 'SUSPENDED', suspensionReasonInput, untilIso);
      setSuccessMsg(`Member ${suspendingMember.name} has been suspended.`);
      setSuspendingMember(null);
      fetchMembers();
      if (selected360Member && selected360Member.member.id === suspendingMember.id) {
        handleView360(suspendingMember.id);
      }
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  const handleReinstateMember = async (memberId: string) => {
    try {
      await api.members.updateStatus(memberId, 'ACTIVE');
      setSuccessMsg('Member has been reinstated to ACTIVE status.');
      fetchMembers();
      if (selected360Member && selected360Member.member.id === memberId) {
        handleView360(memberId);
      }
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  const handleRegisterMember = async (e: React.FormEvent) => {
    e.preventDefault();
    setSuccessMsg(null);
    setErrorMsg(null);
    try {
      const created = await api.members.register({
        name: regName,
        email: regEmail,
        password: regPassword,
        department: regDepartment,
        phone: regPhone,
        maxBorrowLimit: regLimit,
      });
      setSuccessMsg(`Member registered successfully! ID: ${created.memberId} (${created.email})`);
      setShowRegisterMemberModal(false);
      setRegName('');
      setRegEmail('');
      fetchMembers();
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  const handleResetPassword = async (memberId: string) => {
    if (!newPasswordValue.trim()) {
      setErrorMsg('Please enter a new temporary password.');
      return;
    }
    try {
      await api.members.resetPassword(memberId, newPasswordValue.trim());
      setSuccessMsg('Member password updated successfully.');
      setNewPasswordValue('');
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  // -------------------------------------------------------------
  // TAB 3: INVENTORY & COPIES MANAGEMENT
  // -------------------------------------------------------------
  const [inventoryViewMode, setInventoryViewMode] = useState<'copies' | 'titles'>('copies');
  const [allCopies, setAllCopies] = useState<CopyDetail[]>([]);
  const [copyStatusFilter, setCopyStatusFilter] = useState<string>('');
  const [copyBarcodeFilter, setCopyBarcodeFilter] = useState('');
  
  // Physical Copy Edit Modal
  const [editingCopy, setEditingCopy] = useState<BookCopy | null>(null);
  const [editCopyStatus, setEditCopyStatus] = useState<CopyStatus>('AVAILABLE');
  const [editCopyLocation, setEditCopyLocation] = useState('');
  const [editCopyNotes, setEditCopyNotes] = useState('');

  // Catalogue Book Edit Modal
  const [catalogBooks, setCatalogBooks] = useState<Book[]>([]);
  const [editingBook, setEditingBook] = useState<Book | null>(null);
  const [editTitle, setEditTitle] = useState('');
  const [editAuthors, setEditAuthors] = useState('');
  const [editIsbn, setEditIsbn] = useState('');
  const [editPublisher, setEditPublisher] = useState('');
  const [editYear, setEditYear] = useState<number>(2024);

  // Add copies state
  const [selectedBookForCopies, setSelectedBookForCopies] = useState('');
  const [copiesCountToAdd, setCopiesCountToAdd] = useState(2);

  // Add Book state
  const [newTitle, setNewTitle] = useState('');
  const [newAuthor, setNewAuthor] = useState('');
  const [newIsbn, setNewIsbn] = useState('');
  const [newCategory, setNewCategory] = useState('Distributed Systems');
  const [newPublisher, setNewPublisher] = useState('MIT Press');
  const [newYear, setNewYear] = useState<number>(2024);
  const [newCopies, setNewCopies] = useState<number>(3);
  const [newDescription, setNewDescription] = useState('');

  const fetchInventory = async () => {
    try {
      const [copiesData, booksData] = await Promise.all([
        api.books.getAllCopies(undefined, copyStatusFilter || undefined, copyBarcodeFilter || undefined),
        api.books.getAll(0, 100),
      ]);
      setAllCopies(copiesData || []);
      setCatalogBooks(booksData.content || []);
      if (booksData.content?.length > 0 && !selectedBookForCopies) {
        setSelectedBookForCopies(booksData.content[0].id);
      }
    } catch (err: any) {
      console.error(err);
    }
  };

  const handleSaveCopy = async () => {
    if (!editingCopy) return;
    try {
      await api.books.updateCopy(editingCopy.id, {
        status: editCopyStatus,
        location: editCopyLocation,
        conditionNotes: editCopyNotes,
      });
      setSuccessMsg(`Copy barcode ${editingCopy.barcode} updated successfully.`);
      setEditingCopy(null);
      fetchInventory();
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  const handleDeleteCopy = async (copyId: string) => {
    if (!confirm('Are you sure you want to delete this physical copy? This cannot be undone.')) return;
    try {
      await api.books.deleteCopy(copyId);
      setSuccessMsg('Physical copy removed.');
      fetchInventory();
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  const handleOpenEditBook = (book: Book) => {
    setEditingBook(book);
    setEditTitle(book.title);
    setEditAuthors(book.authors?.join(', ') || '');
    setEditIsbn(book.isbn);
    setEditPublisher(book.publisher || '');
    setEditYear(book.publicationYear || 2024);
  };

  const handleSaveBook = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingBook) return;
    try {
      await api.books.update(editingBook.id, {
        title: editTitle,
        isbn: editIsbn,
        authors: editAuthors.split(',').map((s) => s.trim()).filter(Boolean),
        publisher: editPublisher,
        publicationYear: editYear,
      });
      setSuccessMsg(`Book "${editTitle}" metadata updated successfully.`);
      setEditingBook(null);
      fetchInventory();
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  const handleArchiveBook = async (bookId: string) => {
    if (!confirm('Archive this book title? Active physical copies with loans cannot be archived.')) return;
    try {
      await api.books.delete(bookId, false);
      setSuccessMsg('Book title archived successfully.');
      fetchInventory();
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  const handleCreateBook = async (e: React.FormEvent) => {
    e.preventDefault();
    setSuccessMsg(null);
    setErrorMsg(null);
    try {
      const created = await api.books.create({
        title: newTitle,
        authors: newAuthor.split(',').map((s) => s.trim()).filter(Boolean),
        isbn: newIsbn,
        categories: newCategory.split(',').map((s) => s.trim()).filter(Boolean),
        publisher: newPublisher,
        publicationYear: newYear,
        totalCopies: newCopies,
        description: newDescription,
      });
      setSuccessMsg(`Book "${created.title}" added to catalogue with ${newCopies} copies.`);
      setNewTitle('');
      setNewAuthor('');
      setNewIsbn('');
      fetchInventory();
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  const handleAddCopies = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedBookForCopies) return;
    try {
      const updated = await api.books.addCopies(selectedBookForCopies, copiesCountToAdd);
      setSuccessMsg(`Allocated ${copiesCountToAdd} new copies to "${updated.title}".`);
      fetchInventory();
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  // -------------------------------------------------------------
  // TAB 4: ALL LOANS
  // -------------------------------------------------------------
  const [allLoans, setAllLoans] = useState<Loan[]>([]);
  const [loanStatusFilter, setLoanStatusFilter] = useState('');
  const [loanSearchQuery, setLoanSearchQuery] = useState('');

  const fetchAllLoans = async () => {
    try {
      const list = await api.circulation.allLoans(loanStatusFilter || undefined, loanSearchQuery || undefined);
      setAllLoans(list || []);
    } catch (err: any) {
      console.error(err);
    }
  };

  const formatLoanMemberId = (memberId?: string, memberCode?: string) => {
    if (memberCode && memberCode.startsWith('MEM-')) return memberCode;
    if (memberId && memberId.startsWith('MEM-')) return memberId;
    const match = membersList.find((m) => m.id === memberId || m.memberId === memberId);
    if (match && match.memberId) return match.memberId;
    if (memberId && /^\d+$/.test(memberId)) {
      return `MEM-${memberId.slice(-4)}`;
    }
    return memberId || 'MEM-0001';
  };

  // -------------------------------------------------------------
  // TAB 5: ALL RESERVATIONS
  // -------------------------------------------------------------
  const [allReservations, setAllReservations] = useState<Reservation[]>([]);
  const [reservationStatusFilter, setReservationStatusFilter] = useState('');
  
  // Cancel Reservation Modal State
  const [cancellingResId, setCancellingResId] = useState<string | null>(null);
  const [cancelResReason, setCancelResReason] = useState('Member requested cancellation / expired allocation');

  const fetchAllReservations = async () => {
    try {
      const list = await api.reservations.all(reservationStatusFilter || undefined);
      setAllReservations(list || []);
    } catch (err: any) {
      console.error(err);
    }
  };

  const handleFulfillReservation = async (id: string) => {
    try {
      await api.reservations.fulfill(id);
      setSuccessMsg('Reservation marked as fulfilled and copy collected.');
      fetchAllReservations();
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  const handleConfirmCancelReservation = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!cancellingResId) return;
    try {
      await api.reservations.cancelByStaff(cancellingResId, cancelResReason);
      setSuccessMsg('Reservation cancelled successfully.');
      setCancellingResId(null);
      fetchAllReservations();
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  const handleExpireReservation = async (id: string) => {
    try {
      await api.reservations.expire(id);
      setSuccessMsg('Reservation expired and physical copy released.');
      fetchAllReservations();
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  // -------------------------------------------------------------
  // TAB 6: FINES & CASHIER
  // -------------------------------------------------------------
  const [allFines, setAllFines] = useState<Fine[]>([]);
  const [fineStatusFilter, setFineStatusFilter] = useState('');

  // Fine Action Modal State (Collect Cash, Adjust, Waive)
  interface FineActionState {
    type: 'CASH' | 'ADJUST' | 'WAIVE';
    fineId: string;
    amount: number;
    memberId: string;
  }
  const [activeFineAction, setActiveFineAction] = useState<FineActionState | null>(null);
  const [fineCashNotes, setFineCashNotes] = useState('Collected cash at desk counter (Receipt #)');
  const [fineAdjustAmount, setFineAdjustAmount] = useState<number>(0);
  const [fineAdjustReason, setFineAdjustReason] = useState('Partial dispute resolution approved by librarian');
  const [fineWaiveReason, setFineWaiveReason] = useState('First-time waiver approved by administration');

  const fetchAllFines = async () => {
    try {
      const list = await api.fines.all(fineStatusFilter || undefined);
      setAllFines(list || []);
    } catch (err: any) {
      console.error(err);
    }
  };

  const handleOpenCashPaymentModal = (fineId: string, amount: number, memberId: string) => {
    setActiveFineAction({ type: 'CASH', fineId, amount, memberId });
    setFineCashNotes(`Collected cash at desk counter. Receipt #${Math.floor(1000 + Math.random() * 9000)}`);
  };

  const handleOpenAdjustModal = (fineId: string, currentAmount: number, memberId: string) => {
    setActiveFineAction({ type: 'ADJUST', fineId, amount: currentAmount, memberId });
    setFineAdjustAmount(Math.max(0, currentAmount / 2));
    setFineAdjustReason('Partial dispute resolution approved by librarian');
  };

  const handleOpenWaiveModal = (fineId: string, amount: number, memberId: string) => {
    setActiveFineAction({ type: 'WAIVE', fineId, amount, memberId });
    setFineWaiveReason('First-time waiver / Administrative grace approval');
  };

  const handleExecuteFineAction = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeFineAction) return;

    try {
      if (activeFineAction.type === 'CASH') {
        await api.fines.cashPayment(activeFineAction.fineId, fineCashNotes);
        setSuccessMsg(`Cash payment of INR ${activeFineAction.amount.toFixed(2)} recorded.`);
      } else if (activeFineAction.type === 'ADJUST') {
        await api.fines.adjust(activeFineAction.fineId, fineAdjustAmount, fineAdjustReason);
        setSuccessMsg(`Fine adjusted to INR ${fineAdjustAmount.toFixed(2)}.`);
      } else if (activeFineAction.type === 'WAIVE') {
        await api.fines.waive(activeFineAction.fineId, fineWaiveReason);
        setSuccessMsg(`Fine waived successfully.`);
      }

      setActiveFineAction(null);
      fetchAllFines();
      if (selected360Member) {
        handleView360(selected360Member.member.id);
      }
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  // -------------------------------------------------------------
  // TAB 7: CRAWLER JOBS
  // -------------------------------------------------------------
  const [sourceUrl, setSourceUrl] = useState('http://localhost:8080/mock-external-library/books');
  const [isCrawling, setIsCrawling] = useState(false);
  const [crawlerJobs, setCrawlerJobs] = useState<CrawlerJob[]>([]);

  const fetchCrawlerJobs = async () => {
    try {
      const jobs = await api.crawler.getJobs();
      setCrawlerJobs(jobs || []);
    } catch (err: any) {
      console.error(err);
    }
  };

  const handleTriggerCrawler = async () => {
    setIsCrawling(true);
    setSuccessMsg(null);
    setErrorMsg(null);
    try {
      const job = await api.crawler.trigger(sourceUrl);
      setSuccessMsg(`Crawler job ${job.jobId} completed! Parsed: ${job.recordsParsed}, Imported: ${job.recordsImported}, Deduped: ${job.recordsDeduplicated}`);
      fetchCrawlerJobs();
    } catch (err: any) {
      setErrorMsg(err.message);
    } finally {
      setIsCrawling(false);
    }
  };

  // -------------------------------------------------------------
  // TAB 8: AUDIT TRAIL
  // -------------------------------------------------------------
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);

  const fetchAuditLogs = async () => {
    try {
      const logs = await api.auditLogs.getRecent();
      setAuditLogs(logs || []);
    } catch (err: any) {
      console.error(err);
    }
  };

  // -------------------------------------------------------------
  // TAB 9: STAFF & ACCESS (ADMIN ONLY)
  // -------------------------------------------------------------
  const [staffAccounts, setStaffAccounts] = useState<Member[]>([]);
  const [staffName, setStaffName] = useState('');
  const [staffEmail, setStaffEmail] = useState('');
  const [staffPassword, setStaffPassword] = useState('Staff123!');
  const [staffRole, setStaffRole] = useState<Role>('LIBRARIAN');
  const [staffDept, setStaffDept] = useState('Circulation Desk');

  const fetchStaffAccounts = async () => {
    if (!isAdmin) return;
    try {
      const staff = await api.admin.getStaff();
      setStaffAccounts(staff || []);
    } catch (err: any) {
      console.error(err);
    }
  };

  const handleCreateStaff = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const created = await api.admin.createStaff({
        name: staffName,
        email: staffEmail,
        password: staffPassword,
        role: staffRole,
        department: staffDept,
      });
      setSuccessMsg(`Staff account created for ${created.name} (${created.role}).`);
      setStaffName('');
      setStaffEmail('');
      fetchStaffAccounts();
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  const handleToggleStaffRole = async (staffId: string, currentRole: Role, email: string) => {
    if (email === currentUser?.email) {
      setErrorMsg('Self-Protection: Administrators cannot alter or demote their own account role!');
      return;
    }
    const nextRole: Role = currentRole === 'LIBRARIAN' ? 'ADMIN' : 'LIBRARIAN';
    if (!confirm(`Change staff role for ${email} to ${nextRole}?`)) return;
    try {
      await api.admin.updateStaffRole(staffId, nextRole);
      setSuccessMsg(`Staff role updated to ${nextRole}.`);
      fetchStaffAccounts();
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  const handleToggleStaffActive = async (staffId: string, email: string) => {
    if (email === currentUser?.email) {
      setErrorMsg('Self-Protection: Administrators cannot deactivate their own active account!');
      return;
    }
    try {
      await api.admin.toggleStaffStatus(staffId);
      setSuccessMsg(`Staff active status updated for ${email}.`);
      fetchStaffAccounts();
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  // -------------------------------------------------------------
  // TAB 10: SYSTEM CONFIG (ADMIN ONLY)
  // -------------------------------------------------------------
  const [sysConfig, setSysConfig] = useState<SystemConfig>({
    loanPeriodDays: 14,
    defaultBorrowLimit: 5,
    dailyFineRate: 1.5,
    holdExpiryHours: 48,
    maintenanceMode: false,
    maxRenewalCount: 2,
  });

  const fetchSysConfig = async () => {
    try {
      const config = await api.admin.getConfig();
      if (config) setSysConfig(config);
    } catch (err: any) {
      console.error(err);
    }
  };

  const handleSaveConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const updated = await api.admin.updateConfig(sysConfig);
      setSysConfig(updated);
      setSuccessMsg('System configuration and operational policies updated successfully!');
    } catch (err: any) {
      setErrorMsg(err.message);
    }
  };

  // -------------------------------------------------------------
  // INITIAL FETCH & TAB SWITCHING
  // -------------------------------------------------------------
  useEffect(() => {
    if (activeTab === 'circulation') {
      // ready
    } else if (activeTab === 'members') {
      fetchMembers();
    } else if (activeTab === 'inventory') {
      fetchInventory();
    } else if (activeTab === 'loans') {
      if (membersList.length === 0) fetchMembers();
      fetchAllLoans();
    } else if (activeTab === 'reservations') {
      fetchAllReservations();
    } else if (activeTab === 'fines') {
      fetchAllFines();
    } else if (activeTab === 'crawler') {
      fetchCrawlerJobs();
    } else if (activeTab === 'audit') {
      fetchAuditLogs();
    } else if (activeTab === 'staff') {
      fetchStaffAccounts();
    } else if (activeTab === 'config') {
      fetchSysConfig();
    }
  }, [activeTab, memberSearch, memberStatusFilter, copyStatusFilter, copyBarcodeFilter, loanStatusFilter, loanSearchQuery, reservationStatusFilter, fineStatusFilter]);

  return (
    <div className="space-y-6">
      {/* 1. Header with Role Badge */}
      <div className="bg-white rounded-panel p-6 border border-vidya-border shadow-subtle flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold font-display text-vidya-navy">Library Operations & Staff Portal</h1>
            <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold tracking-wide uppercase ${
              isAdmin ? 'bg-purple-100 text-purple-800 border border-purple-200' : 'bg-blue-100 text-blue-800 border border-blue-200'
            }`}>
              {currentUser?.role || 'STAFF'} PORTAL
            </span>
          </div>
          <p className="text-sm text-vidya-text-secondary mt-1">
            Complete administration dashboard for Circulation, Members, Inventory, Holds, Fines, and Security policies.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => {
              setSuccessMsg(null);
              setErrorMsg(null);
              if (activeTab === 'members') fetchMembers();
              if (activeTab === 'inventory') fetchInventory();
              if (activeTab === 'loans') fetchAllLoans();
              if (activeTab === 'reservations') fetchAllReservations();
              if (activeTab === 'fines') fetchAllFines();
              if (activeTab === 'crawler') fetchCrawlerJobs();
              if (activeTab === 'audit') fetchAuditLogs();
              if (activeTab === 'staff') fetchStaffAccounts();
              if (activeTab === 'config') fetchSysConfig();
            }}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-control border border-vidya-border text-xs font-semibold text-vidya-text-secondary hover:text-vidya-text-main bg-white hover:bg-vidya-bg transition-colors cursor-pointer"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Refresh Data</span>
          </button>
        </div>
      </div>

      {/* Global Alerts */}
      {successMsg && (
        <div className="p-4 rounded-control bg-emerald-50 border border-emerald-200 text-emerald-800 text-sm flex items-center justify-between gap-2 shadow-sm animate-fade-in">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            <span>{successMsg}</span>
          </div>
          <button onClick={() => setSuccessMsg(null)} className="text-emerald-600 hover:text-emerald-900 cursor-pointer">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {errorMsg && (
        <div className="p-4 rounded-control bg-rose-50 border border-rose-200 text-rose-800 text-sm flex items-center justify-between gap-2 shadow-sm animate-fade-in">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />
            <span>{errorMsg}</span>
          </div>
          <button onClick={() => setErrorMsg(null)} className="text-rose-600 hover:text-rose-900 cursor-pointer">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* 2. Navigation Tabs */}
      <div className="flex flex-wrap gap-1.5 p-1.5 bg-vidya-blue-surface border border-vidya-border rounded-panel overflow-x-auto">
        <button
          onClick={() => setActiveTab('circulation')}
          className={`flex items-center gap-2 px-3.5 py-2 rounded-control text-xs font-bold transition-all cursor-pointer ${
            activeTab === 'circulation' ? 'bg-white text-vidya-navy shadow-subtle border border-vidya-border' : 'text-slate-600 hover:text-vidya-navy'
          }`}
        >
          <Barcode className="w-4 h-4 text-vidya-blue" />
          <span>Circulation Desk</span>
        </button>

        <button
          onClick={() => setActiveTab('members')}
          className={`flex items-center gap-2 px-3.5 py-2 rounded-control text-xs font-bold transition-all cursor-pointer ${
            activeTab === 'members' ? 'bg-white text-vidya-navy shadow-subtle border border-vidya-border' : 'text-slate-600 hover:text-vidya-navy'
          }`}
        >
          <Users className="w-4 h-4 text-emerald-600" />
          <span>Members (360°)</span>
        </button>

        <button
          onClick={() => setActiveTab('inventory')}
          className={`flex items-center gap-2 px-3.5 py-2 rounded-control text-xs font-bold transition-all cursor-pointer ${
            activeTab === 'inventory' ? 'bg-white text-vidya-navy shadow-subtle border border-vidya-border' : 'text-slate-600 hover:text-vidya-navy'
          }`}
        >
          <Layers className="w-4 h-4 text-indigo-600" />
          <span>Inventory & Copies</span>
        </button>

        <button
          onClick={() => setActiveTab('loans')}
          className={`flex items-center gap-2 px-3.5 py-2 rounded-control text-xs font-bold transition-all cursor-pointer ${
            activeTab === 'loans' ? 'bg-white text-vidya-navy shadow-subtle border border-vidya-border' : 'text-slate-600 hover:text-vidya-navy'
          }`}
        >
          <History className="w-4 h-4 text-amber-600" />
          <span>All Circulation Loans</span>
        </button>

        <button
          onClick={() => setActiveTab('reservations')}
          className={`flex items-center gap-2 px-3.5 py-2 rounded-control text-xs font-bold transition-all cursor-pointer ${
            activeTab === 'reservations' ? 'bg-white text-vidya-navy shadow-subtle border border-vidya-border' : 'text-slate-600 hover:text-vidya-navy'
          }`}
        >
          <Clock className="w-4 h-4 text-purple-600" />
          <span>Reservations & Holds</span>
        </button>

        <button
          onClick={() => setActiveTab('fines')}
          className={`flex items-center gap-2 px-3.5 py-2 rounded-control text-xs font-bold transition-all cursor-pointer ${
            activeTab === 'fines' ? 'bg-white text-vidya-navy shadow-subtle border border-vidya-border' : 'text-slate-600 hover:text-vidya-navy'
          }`}
        >
          <CreditCard className="w-4 h-4 text-rose-600" />
          <span>Fines & Cashier</span>
        </button>

        <button
          onClick={() => setActiveTab('crawler')}
          className={`flex items-center gap-2 px-3.5 py-2 rounded-control text-xs font-bold transition-all cursor-pointer ${
            activeTab === 'crawler' ? 'bg-white text-vidya-navy shadow-subtle border border-vidya-border' : 'text-slate-600 hover:text-vidya-navy'
          }`}
        >
          <Globe className="w-4 h-4 text-cyan-600" />
          <span>Metadata Crawler</span>
        </button>

        <button
          onClick={() => setActiveTab('audit')}
          className={`flex items-center gap-2 px-3.5 py-2 rounded-control text-xs font-bold transition-all cursor-pointer ${
            activeTab === 'audit' ? 'bg-white text-vidya-navy shadow-subtle border border-vidya-border' : 'text-slate-600 hover:text-vidya-navy'
          }`}
        >
          <FileText className="w-4 h-4 text-slate-700" />
          <span>Staff Audit Trail</span>
        </button>

        {/* ADMIN ONLY TABS */}
        {isAdmin && (
          <>
            <button
              onClick={() => setActiveTab('staff')}
              className={`flex items-center gap-2 px-3.5 py-2 rounded-control text-xs font-bold transition-all cursor-pointer ${
                activeTab === 'staff' ? 'bg-purple-900 text-white shadow-subtle' : 'text-purple-700 hover:text-purple-900'
              }`}
            >
              <Shield className="w-4 h-4" />
              <span>Staff & Roles</span>
            </button>

            <button
              onClick={() => setActiveTab('config')}
              className={`flex items-center gap-2 px-3.5 py-2 rounded-control text-xs font-bold transition-all cursor-pointer ${
                activeTab === 'config' ? 'bg-purple-900 text-white shadow-subtle' : 'text-purple-700 hover:text-purple-900'
              }`}
            >
              <Settings className="w-4 h-4" />
              <span>Library Policies</span>
            </button>
          </>
        )}
      </div>

      {/* ------------------------------------------------------------- */}
      {/* VIEW 1: CIRCULATION DESK                                      */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'circulation' && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Checkout Terminal */}
          <div className="bg-white rounded-panel p-6 border border-vidya-border shadow-subtle">
            <div className="flex items-center justify-between pb-4 border-b border-vidya-border">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-control bg-blue-50 text-vidya-blue">
                  <Barcode className="w-5 h-5" />
                </div>
                <div>
                  <h2 className="text-base font-bold text-vidya-navy">Circulation Fast Checkout</h2>
                  <p className="text-xs text-vidya-text-muted">Issue a book copy to a member by barcode</p>
                </div>
              </div>
            </div>

            <form onSubmit={handleStaffCheckout} className="mt-5 space-y-4">
              <div>
                <label className="block text-xs font-semibold text-vidya-text-main mb-1">
                  Member Email, ID, or Identifier
                </label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={checkoutMemberQuery}
                    onChange={(e) => setCheckoutMemberQuery(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        handleVerifyMember();
                      }
                    }}
                    placeholder="e.g. member@ssn.edu.in or MEM-1001"
                    className="flex-1 px-3 py-2 rounded-control border border-vidya-border text-sm focus:border-vidya-blue focus:outline-none"
                    required
                  />
                  <button
                    type="button"
                    onClick={handleVerifyMember}
                    disabled={checkingEligibility}
                    className="px-3.5 py-2 rounded-control bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold transition-colors cursor-pointer"
                  >
                    {checkingEligibility ? 'Verifying...' : 'Verify Member'}
                  </button>
                </div>
              </div>

              {/* Eligibility Verification Card */}
              {memberEligibility && (
                <div className={`p-3 rounded-control border text-xs ${
                  memberEligibility.eligible ? 'bg-emerald-50 border-emerald-200 text-emerald-900' : 'bg-rose-50 border-rose-200 text-rose-900'
                }`}>
                  <div className="flex items-center justify-between font-bold">
                    <span>{memberEligibility.memberName} ({memberEligibility.memberId})</span>
                    <span className="uppercase px-2 py-0.5 rounded text-[10px] bg-white border">
                      {memberEligibility.status}
                    </span>
                  </div>
                  <div className="mt-1 flex gap-4 text-[11px]">
                    <span>Active Loans: <b>{memberEligibility.activeLoans} / {memberEligibility.maxBorrowLimit}</b></span>
                    <span>Unpaid Fines: <b>INR {memberEligibility.unpaidFinesAmount.toFixed(2)}</b></span>
                  </div>
                  {!memberEligibility.eligible && (
                    <div className="mt-1 text-rose-700 font-semibold flex items-center gap-1">
                      <AlertTriangle className="w-3.5 h-3.5" />
                      <span>{memberEligibility.ineligibilityReason}</span>
                    </div>
                  )}
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-vidya-text-main mb-1">
                  Physical Copy Barcode
                </label>
                <input
                  type="text"
                  value={checkoutBarcode}
                  onChange={(e) => setCheckoutBarcode(e.target.value)}
                  placeholder="Scan or enter copy barcode (e.g. BAR-88800-2)"
                  className="w-full px-3 py-2 rounded-control border border-vidya-border text-sm font-mono focus:border-vidya-blue focus:outline-none"
                  required
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-vidya-text-main mb-1">
                    Loan Duration (Days)
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="60"
                    value={checkoutLoanDays}
                    onChange={(e) => setCheckoutLoanDays(parseInt(e.target.value) || 14)}
                    className="w-full px-3 py-2 rounded-control border border-vidya-border text-sm"
                  />
                </div>

                <div className="pt-5">
                  <label className="flex items-center gap-2 cursor-pointer text-xs font-semibold text-vidya-text-main">
                    <input
                      type="checkbox"
                      checked={checkoutOverride}
                      onChange={(e) => setCheckoutOverride(e.target.checked)}
                      className="rounded text-vidya-blue"
                    />
                    <span>Staff Limit Override</span>
                  </label>
                </div>
              </div>

              {checkoutOverride && (
                <div>
                  <label className="block text-xs font-semibold text-amber-800 mb-1">
                    Override Justification (Required for audit log)
                  </label>
                  <input
                    type="text"
                    value={checkoutOverrideReason}
                    onChange={(e) => setCheckoutOverrideReason(e.target.value)}
                    placeholder="e.g. Special course assignment dispensation approved by HOD"
                    className="w-full px-3 py-2 rounded-control border border-amber-300 bg-amber-50 text-xs text-amber-900 focus:outline-none"
                    required={checkoutOverride}
                  />
                </div>
              )}

              <button
                type="submit"
                disabled={loading}
                className="w-full py-2.5 rounded-control bg-vidya-blue hover:bg-vidya-navy text-white text-xs font-bold transition-colors shadow-subtle cursor-pointer"
              >
                {loading ? 'Processing Checkout...' : 'Confirm Checkout & Print Receipt'}
              </button>
            </form>

            {checkedOutReceipt && (
              <div className="mt-4 p-4 rounded-control bg-slate-50 border border-vidya-border text-xs">
                <div className="font-bold text-vidya-navy flex items-center justify-between pb-1 border-b">
                  <span>Checkout Receipt</span>
                  <span className="font-mono">{checkedOutReceipt.loanId}</span>
                </div>
                <div className="mt-2 space-y-1 text-slate-700">
                  <p>Book: <b>{checkedOutReceipt.bookTitle}</b></p>
                  <p>Barcode: <code className="bg-slate-200 px-1 rounded">{checkedOutReceipt.barcode}</code></p>
                  <p>Due Date: <b>{new Date(checkedOutReceipt.dueDate).toLocaleDateString()}</b></p>
                </div>
              </div>
            )}
          </div>

          {/* Return Terminal */}
          <div className="bg-white rounded-panel p-6 border border-vidya-border shadow-subtle">
            <div className="flex items-center justify-between pb-4 border-b border-vidya-border">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-control bg-emerald-50 text-emerald-600">
                  <RotateCw className="w-5 h-5" />
                </div>
                <div>
                  <h2 className="text-base font-bold text-vidya-navy">Check-In / Return Processing</h2>
                  <p className="text-xs text-vidya-text-muted">Scan barcode, record condition, and assess damage fees</p>
                </div>
              </div>
            </div>

            <form onSubmit={handleStaffReturn} className="mt-5 space-y-4">
              <div>
                <label className="block text-xs font-semibold text-vidya-text-main mb-1">
                  Scanned Copy Barcode
                </label>
                <input
                  type="text"
                  value={returnBarcode}
                  onChange={(e) => setReturnBarcode(e.target.value)}
                  placeholder="Scan or enter return barcode (e.g. BAR-88800-2)"
                  className="w-full px-3 py-2 rounded-control border border-vidya-border text-sm font-mono focus:border-emerald-600 focus:outline-none"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-vidya-text-main mb-1">
                  Item Physical Condition
                </label>
                <select
                  value={returnCondition}
                  onChange={(e) => setReturnCondition(e.target.value)}
                  className="w-full px-3 py-2 rounded-control border border-vidya-border text-sm bg-white"
                >
                  <option value="NORMAL">Normal / Good Condition</option>
                  <option value="DAMAGED">Damaged (Torn pages, water damage)</option>
                  <option value="LOST">Lost (Reported lost by member)</option>
                </select>
              </div>

              {returnCondition !== 'NORMAL' && (
                <div className="grid grid-cols-2 gap-4 p-3 rounded-control bg-amber-50 border border-amber-200">
                  <div>
                    <label className="block text-[11px] font-bold text-amber-900 mb-1">
                      Assessed Fee (INR)
                    </label>
                    <input
                      type="number"
                      step="0.5"
                      value={returnDamageFee}
                      onChange={(e) => setReturnDamageFee(parseFloat(e.target.value) || 0)}
                      className="w-full px-2.5 py-1.5 rounded-control border border-amber-300 text-xs bg-white"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-bold text-amber-900 mb-1">
                      Condition Notes
                    </label>
                    <input
                      type="text"
                      value={returnNotes}
                      onChange={(e) => setReturnNotes(e.target.value)}
                      placeholder="e.g. Spine broken, pages missing"
                      className="w-full px-2.5 py-1.5 rounded-control border border-amber-300 text-xs bg-white"
                    />
                  </div>
                </div>
              )}

              <button
                type="submit"
                disabled={loading}
                className="w-full py-2.5 rounded-control bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition-colors shadow-subtle cursor-pointer"
              >
                {loading ? 'Processing Check-In...' : 'Confirm Return & Update Inventory'}
              </button>
            </form>

            {returnReceipt && (
              <div className="mt-4 p-4 rounded-control bg-emerald-50 border border-emerald-200 text-xs text-emerald-950">
                <div className="font-bold flex items-center justify-between pb-1 border-b border-emerald-200">
                  <span>Return Receipt</span>
                  <span className="font-mono">{returnReceipt.loanId}</span>
                </div>
                <div className="mt-2 space-y-1">
                  <p>Status: <b>{returnReceipt.status}</b></p>
                  <p>Overdue Days: <b>{returnReceipt.overdueDays}</b></p>
                  <p>Overdue Fine: <b>INR {returnReceipt.fineAmount?.toFixed(2) || '0.00'}</b></p>
                  {returnReceipt.fineId && (
                    <p className="text-amber-800 font-semibold">Fine Reference: {returnReceipt.fineId}</p>
                  )}
                  {returnReceipt.assignedToReservationId && (
                    <p className="text-purple-800 font-bold">Copy held for waiting reservation: {returnReceipt.assignedToReservationId}</p>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* VIEW 2: MEMBER 360 & MANAGEMENT                               */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'members' && (
        <div className="space-y-6">
          <div className="bg-white rounded-panel p-6 border border-vidya-border shadow-subtle">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-vidya-border">
              <div className="flex items-center gap-2">
                <Users className="w-5 h-5 text-emerald-600" />
                <h2 className="text-base font-bold text-vidya-navy">Member Directory & Account Services</h2>
              </div>
              <button
                onClick={() => setShowRegisterMemberModal(true)}
                className="flex items-center gap-1.5 px-3.5 py-2 rounded-control bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition-colors shadow-subtle cursor-pointer"
              >
                <UserPlus className="w-4 h-4" />
                <span>Register Member at Desk</span>
              </button>
            </div>

            {/* Filter Bar */}
            <div className="mt-4 flex flex-col sm:flex-row gap-3">
              <div className="flex-1 relative">
                <Search className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
                <input
                  type="text"
                  value={memberSearch}
                  onChange={(e) => setMemberSearch(e.target.value)}
                  placeholder="Search by name, email, or member code..."
                  className="w-full pl-9 pr-3 py-2 rounded-control border border-vidya-border text-xs focus:outline-none focus:border-vidya-blue"
                />
              </div>
              <select
                value={memberStatusFilter}
                onChange={(e) => setMemberStatusFilter(e.target.value)}
                className="px-3 py-2 rounded-control border border-vidya-border text-xs bg-white"
              >
                <option value="">All Account Statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="SUSPENDED">Suspended</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </div>

            {/* Members Table */}
            <div className="mt-4 overflow-x-auto border border-vidya-border rounded-control">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-bold border-b border-vidya-border">
                  <tr>
                    <th className="py-2.5 px-3">Member ID</th>
                    <th className="py-2.5 px-3">Full Name</th>
                    <th className="py-2.5 px-3">Email Address</th>
                    <th className="py-2.5 px-3">Department</th>
                    <th className="py-2.5 px-3">Status</th>
                    <th className="py-2.5 px-3">Borrow Limit</th>
                    <th className="py-2.5 px-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-vidya-border text-slate-700">
                  {membersList.map((m) => (
                    <tr key={m.id} className="hover:bg-slate-50 transition-colors">
                      <td className="py-2.5 px-3 font-mono font-bold text-vidya-blue">{m.memberId}</td>
                      <td className="py-2.5 px-3 font-semibold">{m.name}</td>
                      <td className="py-2.5 px-3 text-slate-600">{m.email}</td>
                      <td className="py-2.5 px-3">{m.department || 'General'}</td>
                      <td className="py-2.5 px-3">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          m.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'
                        }`}>
                          {m.status}
                        </span>
                      </td>
                      <td className="py-2.5 px-3">{m.maxBorrowLimit} books</td>
                      <td className="py-2.5 px-3 text-right space-x-1.5 whitespace-nowrap">
                        <button
                          onClick={() => handleView360(m.id)}
                          className="px-2.5 py-1 rounded bg-vidya-blue-surface hover:bg-vidya-blue-light text-vidya-blue font-bold text-[11px] transition-colors cursor-pointer"
                        >
                          360° Profile
                        </button>
                        <button
                          onClick={() => handleOpenEditMember(m)}
                          className="px-2 py-1 rounded bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-[11px] transition-colors cursor-pointer"
                        >
                          Edit Details
                        </button>
                        {m.status === 'ACTIVE' ? (
                          <button
                            onClick={() => handleOpenSuspendModal(m)}
                            className="px-2 py-1 rounded bg-rose-50 text-rose-700 hover:bg-rose-100 text-[11px] font-bold transition-colors cursor-pointer"
                          >
                            Suspend
                          </button>
                        ) : (
                          <button
                            onClick={() => handleReinstateMember(m.id)}
                            className="px-2 py-1 rounded bg-emerald-50 text-emerald-700 hover:bg-emerald-100 text-[11px] font-bold transition-colors cursor-pointer"
                          >
                            Reinstate
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* 360 Member Profile Drawer */}
          {selected360Member && (
            <div className="bg-white rounded-panel p-6 border-2 border-vidya-blue shadow-lg space-y-5 animate-fade-in">
              <div className="flex items-center justify-between pb-4 border-b border-vidya-border">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-full bg-vidya-blue text-white flex items-center justify-center font-bold text-lg">
                    {selected360Member.member.name.charAt(0)}
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-vidya-navy">{selected360Member.member.name}</h3>
                    <p className="text-xs text-slate-500 font-mono">
                      {selected360Member.member.memberId} · {selected360Member.member.email} · {selected360Member.member.department || 'Standard'}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleOpenEditMember(selected360Member.member)}
                    className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded-control text-xs font-bold cursor-pointer"
                  >
                    Edit Details
                  </button>
                  <button
                    onClick={() => setSelected360Member(null)}
                    className="p-1.5 rounded text-slate-400 hover:text-slate-700 cursor-pointer"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
              </div>

              {/* Eligibility & Overview Tiles */}
              <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
                <div className="p-3 rounded-control bg-slate-50 border border-vidya-border">
                  <p className="text-[10px] text-slate-500 font-bold uppercase">Account Status</p>
                  <p className="text-sm font-bold mt-1">{selected360Member.member.status}</p>
                </div>
                <div className="p-3 rounded-control bg-slate-50 border border-vidya-border">
                  <p className="text-[10px] text-slate-500 font-bold uppercase">Active Loans</p>
                  <p className="text-sm font-bold mt-1 text-vidya-blue">
                    {selected360Member.activeLoans.length} / {selected360Member.member.maxBorrowLimit}
                  </p>
                </div>
                <div className="p-3 rounded-control bg-slate-50 border border-vidya-border">
                  <p className="text-[10px] text-slate-500 font-bold uppercase">Outstanding Fines</p>
                  <p className="text-sm font-bold mt-1 text-rose-600">
                    INR {selected360Member.totalUnpaidFines.toFixed(2)}
                  </p>
                </div>
                <div className="p-3 rounded-control bg-slate-50 border border-vidya-border">
                  <p className="text-[10px] text-slate-500 font-bold uppercase">Checkout Eligible</p>
                  <p className={`text-sm font-bold mt-1 ${selected360Member.eligibility.eligible ? 'text-emerald-600' : 'text-rose-600'}`}>
                    {selected360Member.eligibility.eligible ? 'ELIGIBLE' : 'BLOCKED'}
                  </p>
                </div>
              </div>

              {/* Password Reset by Staff */}
              <div className="p-3 rounded-control bg-slate-50 border border-vidya-border flex items-center gap-3">
                <KeyRound className="w-4 h-4 text-slate-600" />
                <span className="text-xs font-semibold">Staff Password Reset:</span>
                <input
                  type="text"
                  value={newPasswordValue}
                  onChange={(e) => setNewPasswordValue(e.target.value)}
                  placeholder="Enter new temporary password"
                  className="px-2.5 py-1 text-xs border rounded-control bg-white"
                />
                <button
                  type="button"
                  onClick={() => handleResetPassword(selected360Member.member.id)}
                  className="px-3 py-1 rounded bg-slate-200 hover:bg-slate-300 text-xs font-bold cursor-pointer"
                >
                  Apply Password
                </button>
              </div>

              {/* Active Loans */}
              <div>
                <h4 className="text-xs font-bold text-vidya-navy uppercase tracking-wider mb-2">Current Active Loans</h4>
                {selected360Member.activeLoans.length === 0 ? (
                  <p className="text-xs text-slate-500 italic">No active loans currently.</p>
                ) : (
                  <div className="border border-vidya-border rounded-control overflow-hidden">
                    <table className="w-full text-xs text-left">
                      <thead className="bg-slate-50 font-bold text-slate-600">
                        <tr>
                          <th className="p-2">Title</th>
                          <th className="p-2">Barcode</th>
                          <th className="p-2">Borrowed</th>
                          <th className="p-2">Due Date</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-vidya-border">
                        {selected360Member.activeLoans.map((l) => (
                          <tr key={l.id}>
                            <td className="p-2 font-semibold">{l.bookTitle}</td>
                            <td className="p-2 font-mono">{l.barcode}</td>
                            <td className="p-2">{new Date(l.borrowedAt).toLocaleDateString()}</td>
                            <td className="p-2 font-bold text-amber-700">{new Date(l.dueDate).toLocaleDateString()}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              {/* Unpaid Fines */}
              <div>
                <h4 className="text-xs font-bold text-vidya-navy uppercase tracking-wider mb-2">Unpaid Fines</h4>
                {selected360Member.unpaidFines.length === 0 ? (
                  <p className="text-xs text-slate-500 italic">No outstanding fines.</p>
                ) : (
                  <div className="border border-vidya-border rounded-control overflow-hidden">
                    <table className="w-full text-xs text-left">
                      <thead className="bg-slate-50 font-bold text-slate-600">
                        <tr>
                          <th className="p-2">Fine ID</th>
                          <th className="p-2">Overdue Days</th>
                          <th className="p-2">Amount</th>
                          <th className="p-2 text-right">Actions</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-vidya-border">
                        {selected360Member.unpaidFines.map((f) => (
                          <tr key={f.id}>
                            <td className="p-2 font-mono font-bold text-vidya-blue">{f.fineId}</td>
                            <td className="p-2">{f.overdueDays} days</td>
                            <td className="p-2 font-bold text-rose-600">INR {f.amount.toFixed(2)}</td>
                            <td className="p-2 text-right space-x-2">
                              <button
                                onClick={() => handleOpenWaiveModal(f.fineId, f.amount, selected360Member.member.id)}
                                className="px-2 py-0.5 rounded text-[10px] bg-purple-50 text-purple-700 hover:bg-purple-100 font-bold cursor-pointer"
                              >
                                Waive
                              </button>
                              <button
                                onClick={() => handleOpenCashPaymentModal(f.fineId, f.amount, selected360Member.member.id)}
                                className="px-2 py-0.5 rounded text-[10px] bg-emerald-100 hover:bg-emerald-200 text-emerald-800 font-bold cursor-pointer"
                              >
                                Collect Cash
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Edit Member Details Modal */}
          {editingMember && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
              <div className="bg-white rounded-panel p-6 max-w-md w-full border border-vidya-border shadow-xl space-y-4">
                <div className="flex items-center justify-between pb-3 border-b">
                  <h3 className="font-bold text-vidya-navy">Edit Member Details ({editingMember.memberId})</h3>
                  <button onClick={() => setEditingMember(null)} className="text-slate-400 hover:text-slate-600 cursor-pointer">
                    <X className="w-4 h-4" />
                  </button>
                </div>
                <form onSubmit={handleSaveMemberEdit} className="space-y-3 text-xs">
                  <div>
                    <label className="font-semibold block mb-1">Full Name</label>
                    <input
                      type="text"
                      required
                      value={editMemberName}
                      onChange={(e) => setEditMemberName(e.target.value)}
                      className="w-full px-3 py-2 border rounded-control"
                    />
                  </div>
                  <div>
                    <label className="font-semibold block mb-1">Email Address</label>
                    <input
                      type="email"
                      required
                      value={editMemberEmail}
                      onChange={(e) => setEditMemberEmail(e.target.value)}
                      className="w-full px-3 py-2 border rounded-control"
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="font-semibold block mb-1">Department</label>
                      <input
                        type="text"
                        value={editMemberDept}
                        onChange={(e) => setEditMemberDept(e.target.value)}
                        className="w-full px-3 py-2 border rounded-control"
                      />
                    </div>
                    <div>
                      <label className="font-semibold block mb-1">Phone</label>
                      <input
                        type="text"
                        value={editMemberPhone}
                        onChange={(e) => setEditMemberPhone(e.target.value)}
                        className="w-full px-3 py-2 border rounded-control"
                      />
                    </div>
                  </div>
                  <div>
                    <label className="font-semibold block mb-1">Maximum Borrow Limit (Books)</label>
                    <input
                      type="number"
                      min="1"
                      max="30"
                      value={editMemberLimit}
                      onChange={(e) => setEditMemberLimit(parseInt(e.target.value) || 5)}
                      className="w-full px-3 py-2 border rounded-control"
                    />
                  </div>
                  <div className="flex justify-end gap-2 pt-2">
                    <button
                      type="button"
                      onClick={() => setEditingMember(null)}
                      className="px-3 py-1.5 rounded-control border text-slate-600 hover:bg-slate-50 font-bold cursor-pointer"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="px-4 py-1.5 rounded-control bg-vidya-blue hover:bg-vidya-navy text-white font-bold cursor-pointer"
                    >
                      Save Changes
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {/* Suspend Member Modal */}
          {suspendingMember && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
              <div className="bg-white rounded-panel p-6 max-w-md w-full border border-vidya-border shadow-xl space-y-4">
                <div className="flex items-center justify-between pb-3 border-b border-rose-100">
                  <div className="flex items-center gap-2 text-rose-700 font-bold">
                    <UserX className="w-5 h-5" />
                    <span>Suspend Member Account</span>
                  </div>
                  <button onClick={() => setSuspendingMember(null)} className="text-slate-400 hover:text-slate-600 cursor-pointer">
                    <X className="w-4 h-4" />
                  </button>
                </div>
                <form onSubmit={handleConfirmSuspension} className="space-y-3 text-xs">
                  <p className="text-slate-600">
                    Suspending <b>{suspendingMember.name}</b> ({suspendingMember.memberId}) will block checkouts and hold reservations until reinstated.
                  </p>
                  <div>
                    <label className="font-semibold block mb-1 text-slate-700">Suspension Reason (Recorded in Audit Trail)</label>
                    <textarea
                      required
                      rows={2}
                      value={suspensionReasonInput}
                      onChange={(e) => setSuspensionReasonInput(e.target.value)}
                      placeholder="e.g. Failure to return rare collection manuscript on time"
                      className="w-full px-3 py-2 border rounded-control"
                    />
                  </div>
                  <div>
                    <label className="font-semibold block mb-1 text-slate-700">Suspension End Date</label>
                    <input
                      type="date"
                      value={suspensionUntilDate}
                      onChange={(e) => setSuspensionUntilDate(e.target.value)}
                      className="w-full px-3 py-2 border rounded-control"
                    />
                  </div>
                  <div className="flex justify-end gap-2 pt-2">
                    <button
                      type="button"
                      onClick={() => setSuspendingMember(null)}
                      className="px-3 py-1.5 rounded-control border text-slate-600 hover:bg-slate-50 font-bold cursor-pointer"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="px-4 py-1.5 rounded-control bg-rose-600 hover:bg-rose-700 text-white font-bold cursor-pointer"
                    >
                      Confirm Suspension
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {/* Register Member Modal */}
          {showRegisterMemberModal && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
              <div className="bg-white rounded-panel p-6 max-w-md w-full border border-vidya-border shadow-xl space-y-4">
                <div className="flex items-center justify-between pb-3 border-b">
                  <h3 className="font-bold text-vidya-navy">Register New Member at Desk</h3>
                  <button onClick={() => setShowRegisterMemberModal(false)} className="text-slate-400 hover:text-slate-600 cursor-pointer">
                    <X className="w-4 h-4" />
                  </button>
                </div>
                <form onSubmit={handleRegisterMember} className="space-y-3 text-xs">
                  <div>
                    <label className="font-semibold block mb-1">Full Name</label>
                    <input
                      type="text"
                      required
                      value={regName}
                      onChange={(e) => setRegName(e.target.value)}
                      placeholder="e.g. Maya Sundaram"
                      className="w-full px-3 py-2 border rounded-control"
                    />
                  </div>
                  <div>
                    <label className="font-semibold block mb-1">Email Address</label>
                    <input
                      type="email"
                      required
                      value={regEmail}
                      onChange={(e) => setRegEmail(e.target.value)}
                      placeholder="e.g. maya@ssn.edu.in"
                      className="w-full px-3 py-2 border rounded-control"
                    />
                  </div>
                  <div>
                    <label className="font-semibold block mb-1">Initial Password</label>
                    <input
                      type="text"
                      required
                      value={regPassword}
                      onChange={(e) => setRegPassword(e.target.value)}
                      className="w-full px-3 py-2 border rounded-control font-mono"
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="font-semibold block mb-1">Department</label>
                      <input
                        type="text"
                        value={regDepartment}
                        onChange={(e) => setRegDepartment(e.target.value)}
                        className="w-full px-3 py-2 border rounded-control"
                      />
                    </div>
                    <div>
                      <label className="font-semibold block mb-1">Borrow Limit</label>
                      <input
                        type="number"
                        min="1"
                        max="20"
                        value={regLimit}
                        onChange={(e) => setRegLimit(parseInt(e.target.value) || 5)}
                        className="w-full px-3 py-2 border rounded-control"
                      />
                    </div>
                  </div>
                  <button
                    type="submit"
                    className="w-full py-2.5 rounded-control bg-emerald-600 hover:bg-emerald-700 text-white font-bold cursor-pointer"
                  >
                    Confirm Registration
                  </button>
                </form>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* VIEW 3: INVENTORY & COPIES                                    */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'inventory' && (
        <div className="space-y-6">
          {/* Sub-View Switcher Bar */}
          <div className="bg-white rounded-panel p-4 border border-vidya-border shadow-subtle flex flex-col md:flex-row gap-4 justify-between items-start md:items-center">
            <div className="flex items-center gap-3">
              <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Inventory View:</span>
              <div className="flex bg-slate-100 p-1 rounded-control gap-1">
                <button
                  type="button"
                  onClick={() => setInventoryViewMode('copies')}
                  className={`px-3 py-1 rounded text-xs font-bold transition-all cursor-pointer ${
                    inventoryViewMode === 'copies' ? 'bg-white text-vidya-navy shadow-xs' : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  Physical Copies ({allCopies.length})
                </button>
                <button
                  type="button"
                  onClick={() => setInventoryViewMode('titles')}
                  className={`px-3 py-1 rounded text-xs font-bold transition-all cursor-pointer ${
                    inventoryViewMode === 'titles' ? 'bg-white text-vidya-navy shadow-xs' : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  Catalogue Titles ({catalogBooks.length})
                </button>
              </div>
            </div>

            {inventoryViewMode === 'copies' && (
              <div className="flex gap-2 w-full md:w-auto">
                <input
                  type="text"
                  value={copyBarcodeFilter}
                  onChange={(e) => setCopyBarcodeFilter(e.target.value)}
                  placeholder="Filter barcode (BAR-...)"
                  className="px-3 py-1.5 rounded-control border text-xs focus:outline-none"
                />
                <select
                  value={copyStatusFilter}
                  onChange={(e) => setCopyStatusFilter(e.target.value)}
                  className="px-3 py-1.5 rounded-control border text-xs bg-white"
                >
                  <option value="">All Statuses</option>
                  <option value="AVAILABLE">Available</option>
                  <option value="BORROWED">Borrowed</option>
                  <option value="RESERVED_FOR_PICKUP">Reserved</option>
                  <option value="DAMAGED">Damaged</option>
                  <option value="LOST">Lost</option>
                  <option value="UNDER_REPAIR">Under Repair</option>
                  <option value="WITHDRAWN">Withdrawn</option>
                  <option value="MISSING">Missing</option>
                </select>
              </div>
            )}
          </div>

          {/* VIEW MODE: PHYSICAL COPIES TABLE */}
          {inventoryViewMode === 'copies' && (
            <div className="bg-white rounded-panel border border-vidya-border shadow-subtle overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 text-slate-600 font-bold border-b border-vidya-border">
                    <tr>
                      <th className="py-2.5 px-3">Barcode</th>
                      <th className="py-2.5 px-3">Book Title</th>
                      <th className="py-2.5 px-3">Location / Shelf</th>
                      <th className="py-2.5 px-3">Status</th>
                      <th className="py-2.5 px-3">Condition Notes</th>
                      <th className="py-2.5 px-3 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-vidya-border text-slate-700">
                    {allCopies.map((item) => (
                      <tr key={item.copy.id} className="hover:bg-slate-50 transition-colors">
                        <td className="py-2.5 px-3 font-mono font-bold text-vidya-blue">{item.copy.barcode}</td>
                        <td className="py-2.5 px-3 font-semibold">{item.bookTitle}</td>
                        <td className="py-2.5 px-3 text-slate-500">{item.copy.location || 'Central Stacks'}</td>
                        <td className="py-2.5 px-3">
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            item.copy.status === 'AVAILABLE' ? 'bg-emerald-100 text-emerald-800' :
                            item.copy.status === 'BORROWED' ? 'bg-blue-100 text-blue-800' :
                            item.copy.status === 'DAMAGED' ? 'bg-rose-100 text-rose-800' :
                            item.copy.status === 'LOST' ? 'bg-red-200 text-red-900' :
                            'bg-amber-100 text-amber-800'
                          }`}>
                            {item.copy.status}
                          </span>
                        </td>
                        <td className="py-2.5 px-3 text-slate-500">{item.copy.conditionNotes || 'None'}</td>
                        <td className="py-2.5 px-3 text-right space-x-2">
                          <button
                            onClick={() => {
                              setEditingCopy(item.copy);
                              setEditCopyStatus(item.copy.status);
                              setEditCopyLocation(item.copy.location || 'Central Stacks');
                              setEditCopyNotes(item.copy.conditionNotes || '');
                            }}
                            className="px-2.5 py-1 rounded bg-slate-100 hover:bg-slate-200 text-slate-800 font-bold text-[11px] cursor-pointer"
                          >
                            Edit Copy
                          </button>
                          <button
                            onClick={() => handleDeleteCopy(item.copy.id)}
                            className="px-2 py-1 rounded text-rose-600 hover:bg-rose-50 text-[11px] font-bold cursor-pointer"
                          >
                            Delete
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* VIEW MODE: CATALOGUE TITLES TABLE */}
          {inventoryViewMode === 'titles' && (
            <div className="bg-white rounded-panel border border-vidya-border shadow-subtle overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 text-slate-600 font-bold border-b border-vidya-border">
                    <tr>
                      <th className="py-2.5 px-3">Title</th>
                      <th className="py-2.5 px-3">Authors</th>
                      <th className="py-2.5 px-3">ISBN</th>
                      <th className="py-2.5 px-3">Publisher & Year</th>
                      <th className="py-2.5 px-3">Available / Total</th>
                      <th className="py-2.5 px-3 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-vidya-border text-slate-700">
                    {catalogBooks.map((b) => (
                      <tr key={b.id} className="hover:bg-slate-50 transition-colors">
                        <td className="py-2.5 px-3 font-semibold text-vidya-navy">{b.title}</td>
                        <td className="py-2.5 px-3 text-slate-600">{b.authors?.join(', ')}</td>
                        <td className="py-2.5 px-3 font-mono text-slate-500">{b.isbn}</td>
                        <td className="py-2.5 px-3 text-slate-500">{b.publisher} ({b.publicationYear})</td>
                        <td className="py-2.5 px-3 font-semibold">
                          <span className="text-emerald-700">{b.availableCopies}</span> / {b.totalCopies} copies
                        </td>
                        <td className="py-2.5 px-3 text-right space-x-2">
                          <button
                            onClick={() => handleOpenEditBook(b)}
                            className="px-2.5 py-1 rounded bg-vidya-blue-surface hover:bg-vidya-blue-light text-vidya-blue font-bold text-[11px] cursor-pointer"
                          >
                            Edit Title
                          </button>
                          <button
                            onClick={() => handleArchiveBook(b.id)}
                            className="px-2 py-1 rounded bg-amber-50 hover:bg-amber-100 text-amber-800 font-bold text-[11px] cursor-pointer"
                          >
                            Archive
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Add Book & Add Copies Row */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Add Book Title */}
            <div className="bg-white rounded-panel p-6 border border-vidya-border shadow-subtle">
              <h3 className="font-bold text-sm text-vidya-navy flex items-center gap-2 mb-4">
                <BookPlus className="w-4 h-4 text-vidya-blue" />
                <span>Add New Book to Catalogue</span>
              </h3>
              <form onSubmit={handleCreateBook} className="space-y-3 text-xs">
                <div>
                  <label className="font-semibold block mb-1">Title</label>
                  <input
                    type="text"
                    required
                    value={newTitle}
                    onChange={(e) => setNewTitle(e.target.value)}
                    placeholder="e.g. Distributed Systems Architecture"
                    className="w-full px-3 py-2 border rounded-control"
                  />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="font-semibold block mb-1">Author(s)</label>
                    <input
                      type="text"
                      required
                      value={newAuthor}
                      onChange={(e) => setNewAuthor(e.target.value)}
                      placeholder="e.g. Leslie Lamport"
                      className="w-full px-3 py-2 border rounded-control"
                    />
                  </div>
                  <div>
                    <label className="font-semibold block mb-1">ISBN</label>
                    <input
                      type="text"
                      required
                      value={newIsbn}
                      onChange={(e) => setNewIsbn(e.target.value)}
                      placeholder="978-0-13-468599-1"
                      className="w-full px-3 py-2 border rounded-control"
                    />
                  </div>
                </div>
                <div className="grid grid-cols-3 gap-3">
                  <div>
                    <label className="font-semibold block mb-1">Category</label>
                    <input
                      type="text"
                      value={newCategory}
                      onChange={(e) => setNewCategory(e.target.value)}
                      className="w-full px-3 py-2 border rounded-control"
                    />
                  </div>
                  <div>
                    <label className="font-semibold block mb-1">Year</label>
                    <input
                      type="number"
                      value={newYear}
                      onChange={(e) => setNewYear(parseInt(e.target.value) || 2024)}
                      className="w-full px-3 py-2 border rounded-control"
                    />
                  </div>
                  <div>
                    <label className="font-semibold block mb-1">Copies</label>
                    <input
                      type="number"
                      min="1"
                      value={newCopies}
                      onChange={(e) => setNewCopies(parseInt(e.target.value) || 1)}
                      className="w-full px-3 py-2 border rounded-control"
                    />
                  </div>
                </div>
                <button
                  type="submit"
                  className="w-full py-2 rounded-control bg-vidya-blue hover:bg-vidya-navy text-white font-bold cursor-pointer"
                >
                  Create Book & Generate Copies
                </button>
              </form>
            </div>

            {/* Allocate Copies to Existing Title */}
            <div className="bg-white rounded-panel p-6 border border-vidya-border shadow-subtle">
              <h3 className="font-bold text-sm text-vidya-navy flex items-center gap-2 mb-4">
                <CopyPlus className="w-4 h-4 text-emerald-600" />
                <span>Allocate Additional Physical Copies</span>
              </h3>
              <form onSubmit={handleAddCopies} className="space-y-4 text-xs">
                <div>
                  <label className="font-semibold block mb-1">Select Catalogue Title</label>
                  <select
                    value={selectedBookForCopies}
                    onChange={(e) => setSelectedBookForCopies(e.target.value)}
                    className="w-full px-3 py-2 border rounded-control bg-white"
                  >
                    {catalogBooks.map((b) => (
                      <option key={b.id} value={b.id}>
                        {b.title} (Currently: {b.totalCopies} copies)
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="font-semibold block mb-1">Number of Physical Copies to Allocate</label>
                  <input
                    type="number"
                    min="1"
                    max="20"
                    value={copiesCountToAdd}
                    onChange={(e) => setCopiesCountToAdd(parseInt(e.target.value) || 1)}
                    className="w-full px-3 py-2 border rounded-control"
                  />
                </div>
                <button
                  type="submit"
                  className="w-full py-2 rounded-control bg-emerald-600 hover:bg-emerald-700 text-white font-bold cursor-pointer"
                >
                  Generate Barcodes & Add Copies
                </button>
              </form>
            </div>
          </div>

          {/* Edit Copy Modal */}
          {editingCopy && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
              <div className="bg-white rounded-panel p-6 max-w-sm w-full border border-vidya-border shadow-xl space-y-4">
                <div className="flex items-center justify-between pb-2 border-b">
                  <h3 className="font-bold text-vidya-navy text-sm">Edit Copy {editingCopy.barcode}</h3>
                  <button onClick={() => setEditingCopy(null)} className="cursor-pointer">
                    <X className="w-4 h-4 text-slate-400" />
                  </button>
                </div>
                <div className="space-y-3 text-xs">
                  <div>
                    <label className="font-semibold block mb-1">Status</label>
                    <select
                      value={editCopyStatus}
                      onChange={(e) => setEditCopyStatus(e.target.value as CopyStatus)}
                      className="w-full px-3 py-2 border rounded-control bg-white"
                    >
                      <option value="AVAILABLE">AVAILABLE</option>
                      <option value="BORROWED">BORROWED</option>
                      <option value="RESERVED_FOR_PICKUP">RESERVED_FOR_PICKUP</option>
                      <option value="DAMAGED">DAMAGED</option>
                      <option value="LOST">LOST</option>
                      <option value="UNDER_REPAIR">UNDER_REPAIR</option>
                      <option value="WITHDRAWN">WITHDRAWN</option>
                      <option value="MISSING">MISSING</option>
                    </select>
                  </div>
                  <div>
                    <label className="font-semibold block mb-1">Location / Shelf</label>
                    <input
                      type="text"
                      value={editCopyLocation}
                      onChange={(e) => setEditCopyLocation(e.target.value)}
                      placeholder="e.g. 2nd Floor Shelf B-4"
                      className="w-full px-3 py-2 border rounded-control"
                    />
                  </div>
                  <div>
                    <label className="font-semibold block mb-1">Condition Notes</label>
                    <input
                      type="text"
                      value={editCopyNotes}
                      onChange={(e) => setEditCopyNotes(e.target.value)}
                      placeholder="e.g. Binding repaired on 2026-10"
                      className="w-full px-3 py-2 border rounded-control"
                    />
                  </div>
                  <button
                    onClick={handleSaveCopy}
                    className="w-full py-2 rounded-control bg-vidya-blue hover:bg-vidya-navy text-white font-bold cursor-pointer"
                  >
                    Save Changes
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Edit Book Modal */}
          {editingBook && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
              <div className="bg-white rounded-panel p-6 max-w-md w-full border border-vidya-border shadow-xl space-y-4">
                <div className="flex items-center justify-between pb-2 border-b">
                  <h3 className="font-bold text-vidya-navy text-sm">Edit Book Title</h3>
                  <button onClick={() => setEditingBook(null)} className="cursor-pointer">
                    <X className="w-4 h-4 text-slate-400" />
                  </button>
                </div>
                <form onSubmit={handleSaveBook} className="space-y-3 text-xs">
                  <div>
                    <label className="font-semibold block mb-1">Title</label>
                    <input
                      type="text"
                      required
                      value={editTitle}
                      onChange={(e) => setEditTitle(e.target.value)}
                      className="w-full px-3 py-2 border rounded-control"
                    />
                  </div>
                  <div>
                    <label className="font-semibold block mb-1">Authors (comma separated)</label>
                    <input
                      type="text"
                      required
                      value={editAuthors}
                      onChange={(e) => setEditAuthors(e.target.value)}
                      className="w-full px-3 py-2 border rounded-control"
                    />
                  </div>
                  <div>
                    <label className="font-semibold block mb-1">ISBN</label>
                    <input
                      type="text"
                      required
                      value={editIsbn}
                      onChange={(e) => setEditIsbn(e.target.value)}
                      className="w-full px-3 py-2 border rounded-control font-mono"
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="font-semibold block mb-1">Publisher</label>
                      <input
                        type="text"
                        value={editPublisher}
                        onChange={(e) => setEditPublisher(e.target.value)}
                        className="w-full px-3 py-2 border rounded-control"
                      />
                    </div>
                    <div>
                      <label className="font-semibold block mb-1">Year</label>
                      <input
                        type="number"
                        value={editYear}
                        onChange={(e) => setEditYear(parseInt(e.target.value) || 2024)}
                        className="w-full px-3 py-2 border rounded-control"
                      />
                    </div>
                  </div>
                  <div className="flex justify-end gap-2 pt-2">
                    <button
                      type="button"
                      onClick={() => setEditingBook(null)}
                      className="px-3 py-1.5 rounded-control border text-slate-600 hover:bg-slate-50 font-bold cursor-pointer"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="px-4 py-1.5 rounded-control bg-vidya-blue hover:bg-vidya-navy text-white font-bold cursor-pointer"
                    >
                      Save Book Metadata
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* VIEW 4: ALL CIRCULATION LOANS                                 */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'loans' && (
        <div className="space-y-4">
          <div className="bg-white rounded-panel p-6 border border-vidya-border shadow-subtle flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <h2 className="text-base font-bold text-vidya-navy flex items-center gap-2">
                <History className="w-5 h-5 text-amber-600" />
                <span>All Active & Historical Circulation Loans</span>
              </h2>
              <p className="text-xs text-vidya-text-muted mt-0.5">Global audit and tracking of all borrowed materials</p>
            </div>
            <div className="flex gap-2">
              <input
                type="text"
                value={loanSearchQuery}
                onChange={(e) => setLoanSearchQuery(e.target.value)}
                placeholder="Search title or barcode..."
                className="px-3 py-1.5 rounded-control border text-xs focus:outline-none"
              />
              <select
                value={loanStatusFilter}
                onChange={(e) => setLoanStatusFilter(e.target.value)}
                className="px-3 py-1.5 rounded-control border text-xs bg-white"
              >
                <option value="">All Loans</option>
                <option value="ACTIVE">Active Loans</option>
                <option value="RETURNED">Returned</option>
              </select>
            </div>
          </div>

          <div className="bg-white rounded-panel border border-vidya-border shadow-subtle overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-bold border-b border-vidya-border">
                  <tr>
                    <th className="py-2.5 px-3">Loan ID</th>
                    <th className="py-2.5 px-3">Book Title</th>
                    <th className="py-2.5 px-3">Barcode</th>
                    <th className="py-2.5 px-3">Member ID</th>
                    <th className="py-2.5 px-3">Borrowed At</th>
                    <th className="py-2.5 px-3">Due Date</th>
                    <th className="py-2.5 px-3">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-vidya-border text-slate-700">
                  {allLoans.map((l) => (
                    <tr key={l.id} className="hover:bg-slate-50 transition-colors">
                      <td className="py-2.5 px-3 font-mono font-bold text-vidya-blue">{l.loanId}</td>
                      <td className="py-2.5 px-3 font-semibold">{l.bookTitle}</td>
                      <td className="py-2.5 px-3 font-mono">{l.barcode}</td>
                      <td className="py-2.5 px-3 font-mono font-bold text-vidya-blue">{formatLoanMemberId(l.memberId, l.memberCode)}</td>
                      <td className="py-2.5 px-3">{new Date(l.borrowedAt).toLocaleDateString()}</td>
                      <td className="py-2.5 px-3 font-bold text-amber-700">{new Date(l.dueDate).toLocaleDateString()}</td>
                      <td className="py-2.5 px-3">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          l.status === 'ACTIVE' ? 'bg-blue-100 text-blue-800' : 'bg-slate-100 text-slate-700'
                        }`}>
                          {l.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* VIEW 5: RESERVATIONS & HOLDS                                  */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'reservations' && (
        <div className="space-y-4">
          <div className="bg-white rounded-panel p-6 border border-vidya-border shadow-subtle flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <h2 className="text-base font-bold text-vidya-navy flex items-center gap-2">
                <Clock className="w-5 h-5 text-purple-600" />
                <span>Hold Queue & Reservation Management</span>
              </h2>
              <p className="text-xs text-vidya-text-muted mt-0.5">Manage FIFO queues, hold shelf allocations, and expirations</p>
            </div>
            <select
              value={reservationStatusFilter}
              onChange={(e) => setReservationStatusFilter(e.target.value)}
              className="px-3 py-1.5 rounded-control border text-xs bg-white"
            >
              <option value="">All Hold Statuses</option>
              <option value="PENDING">Pending in Queue</option>
              <option value="READY_FOR_PICKUP">Ready for Pickup</option>
              <option value="FULFILLED">Fulfilled</option>
              <option value="CANCELLED">Cancelled</option>
              <option value="EXPIRED">Expired</option>
            </select>
          </div>

          <div className="bg-white rounded-panel border border-vidya-border shadow-subtle overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-bold border-b border-vidya-border">
                  <tr>
                    <th className="py-2.5 px-3">Hold ID</th>
                    <th className="py-2.5 px-3">Book Title</th>
                    <th className="py-2.5 px-3">Member Email</th>
                    <th className="py-2.5 px-3">Queue Position</th>
                    <th className="py-2.5 px-3">Status</th>
                    <th className="py-2.5 px-3">Created</th>
                    <th className="py-2.5 px-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-vidya-border text-slate-700">
                  {allReservations.map((r) => (
                    <tr key={r.id} className="hover:bg-slate-50 transition-colors">
                      <td className="py-2.5 px-3 font-mono font-bold text-vidya-blue">{r.reservationId}</td>
                      <td className="py-2.5 px-3 font-semibold">{r.bookTitle}</td>
                      <td className="py-2.5 px-3 text-slate-600">{r.memberEmail}</td>
                      <td className="py-2.5 px-3 font-bold text-indigo-700">#{r.queuePosition}</td>
                      <td className="py-2.5 px-3">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          r.status === 'READY_FOR_PICKUP' ? 'bg-purple-100 text-purple-800 animate-pulse' :
                          r.status === 'PENDING' ? 'bg-amber-100 text-amber-800' :
                          r.status === 'FULFILLED' ? 'bg-emerald-100 text-emerald-800' :
                          'bg-slate-100 text-slate-700'
                        }`}>
                          {r.status}
                        </span>
                      </td>
                      <td className="py-2.5 px-3">{new Date(r.createdAt).toLocaleDateString()}</td>
                      <td className="py-2.5 px-3 text-right space-x-2">
                        {r.status === 'READY_FOR_PICKUP' && (
                          <button
                            onClick={() => handleFulfillReservation(r.reservationId)}
                            className="px-2 py-1 rounded bg-emerald-100 text-emerald-800 hover:bg-emerald-200 text-[11px] font-bold cursor-pointer"
                          >
                            Mark Picked Up
                          </button>
                        )}
                        {(r.status === 'PENDING' || r.status === 'READY_FOR_PICKUP') && (
                          <button
                            onClick={() => {
                              setCancellingResId(r.reservationId);
                              setCancelResReason('Member requested cancellation / expired hold');
                            }}
                            className="px-2 py-1 rounded bg-rose-50 text-rose-700 hover:bg-rose-100 text-[11px] font-bold cursor-pointer"
                          >
                            Cancel
                          </button>
                        )}
                        {r.status === 'READY_FOR_PICKUP' && (
                          <button
                            onClick={() => handleExpireReservation(r.reservationId)}
                            className="px-2 py-1 rounded bg-slate-100 text-slate-700 hover:bg-slate-200 text-[11px] font-bold cursor-pointer"
                          >
                            Expire Hold
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Cancel Reservation Modal */}
          {cancellingResId && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
              <div className="bg-white rounded-panel p-6 max-w-sm w-full border border-vidya-border shadow-xl space-y-4">
                <div className="flex items-center justify-between pb-2 border-b">
                  <h3 className="font-bold text-vidya-navy text-sm">Cancel Hold ({cancellingResId})</h3>
                  <button onClick={() => setCancellingResId(null)} className="cursor-pointer">
                    <X className="w-4 h-4 text-slate-400" />
                  </button>
                </div>
                <form onSubmit={handleConfirmCancelReservation} className="space-y-3 text-xs">
                  <div>
                    <label className="font-semibold block mb-1">Cancellation Reason</label>
                    <input
                      type="text"
                      required
                      value={cancelResReason}
                      onChange={(e) => setCancelResReason(e.target.value)}
                      className="w-full px-3 py-2 border rounded-control"
                    />
                  </div>
                  <div className="flex justify-end gap-2 pt-2">
                    <button
                      type="button"
                      onClick={() => setCancellingResId(null)}
                      className="px-3 py-1.5 rounded-control border text-slate-600 hover:bg-slate-50 font-bold cursor-pointer"
                    >
                      Dismiss
                    </button>
                    <button
                      type="submit"
                      className="px-4 py-1.5 rounded-control bg-rose-600 hover:bg-rose-700 text-white font-bold cursor-pointer"
                    >
                      Confirm Cancel
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* VIEW 6: FINES & CASHIER                                       */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'fines' && (
        <div className="space-y-4">
          <div className="bg-white rounded-panel p-6 border border-vidya-border shadow-subtle flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <h2 className="text-base font-bold text-vidya-navy flex items-center gap-2">
                <CreditCard className="w-5 h-5 text-rose-600" />
                <span>Library Cashier & Fine Reconciliation</span>
              </h2>
              <p className="text-xs text-vidya-text-muted mt-0.5">Collect cash at desk, adjust disputable fines, or grant waivers</p>
            </div>
            <select
              value={fineStatusFilter}
              onChange={(e) => setFineStatusFilter(e.target.value)}
              className="px-3 py-1.5 rounded-control border text-xs bg-white"
            >
              <option value="">All Fines</option>
              <option value="UNPAID">Unpaid Dues</option>
              <option value="PAID">Paid / Settled</option>
              <option value="WAIVED">Waived by Staff</option>
            </select>
          </div>

          <div className="bg-white rounded-panel border border-vidya-border shadow-subtle overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-bold border-b border-vidya-border">
                  <tr>
                    <th className="py-2.5 px-3">Fine Ref</th>
                    <th className="py-2.5 px-3">Member ID</th>
                    <th className="py-2.5 px-3">Overdue Days</th>
                    <th className="py-2.5 px-3">Amount</th>
                    <th className="py-2.5 px-3">Status</th>
                    <th className="py-2.5 px-3">Payment / Notes</th>
                    <th className="py-2.5 px-3 text-right">Cashier Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-vidya-border text-slate-700">
                  {allFines.map((f) => (
                    <tr key={f.id} className="hover:bg-slate-50 transition-colors">
                      <td className="py-2.5 px-3 font-mono font-bold text-vidya-blue">{f.fineId}</td>
                      <td className="py-2.5 px-3 font-mono text-slate-600">{f.memberId}</td>
                      <td className="py-2.5 px-3">{f.overdueDays} days</td>
                      <td className="py-2.5 px-3 font-bold text-rose-600">INR {f.amount.toFixed(2)}</td>
                      <td className="py-2.5 px-3">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          f.status === 'PAID' ? 'bg-emerald-100 text-emerald-800' :
                          f.status === 'WAIVED' ? 'bg-purple-100 text-purple-800' :
                          'bg-rose-100 text-rose-800'
                        }`}>
                          {f.status}
                        </span>
                      </td>
                      <td className="py-2.5 px-3 text-slate-500">{f.notes || f.waiverReason || '-'}</td>
                      <td className="py-2.5 px-3 text-right space-x-2">
                        {f.status === 'UNPAID' && (
                          <>
                            <button
                              onClick={() => handleOpenCashPaymentModal(f.fineId, f.amount, f.memberId)}
                              className="px-2.5 py-1 rounded bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-[11px] cursor-pointer"
                            >
                              Collect Cash
                            </button>
                            <button
                              onClick={() => handleOpenAdjustModal(f.fineId, f.amount, f.memberId)}
                              className="px-2 py-1 rounded bg-slate-100 hover:bg-slate-200 text-slate-800 font-bold text-[11px] cursor-pointer"
                            >
                              Adjust
                            </button>
                            <button
                              onClick={() => handleOpenWaiveModal(f.fineId, f.amount, f.memberId)}
                              className="px-2 py-1 rounded bg-purple-50 hover:bg-purple-100 text-purple-700 font-bold text-[11px] cursor-pointer"
                            >
                              Waive
                            </button>
                          </>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Unified Fine Action Modal (Cash, Adjust, Waive) */}
          {activeFineAction && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
              <div className="bg-white rounded-panel p-6 max-w-sm w-full border border-vidya-border shadow-xl space-y-4">
                <div className="flex items-center justify-between pb-2 border-b">
                  <h3 className="font-bold text-vidya-navy text-sm">
                    {activeFineAction.type === 'CASH' && 'Record Cash Payment'}
                    {activeFineAction.type === 'ADJUST' && 'Adjust Fine Amount'}
                    {activeFineAction.type === 'WAIVE' && 'Waive Fine'}
                  </h3>
                  <button onClick={() => setActiveFineAction(null)} className="cursor-pointer">
                    <X className="w-4 h-4 text-slate-400" />
                  </button>
                </div>

                <form onSubmit={handleExecuteFineAction} className="space-y-3 text-xs">
                  <div className="p-2.5 rounded bg-slate-50 border text-slate-600">
                    <p>Fine ID: <b className="font-mono text-vidya-navy">{activeFineAction.fineId}</b></p>
                    <p>Current Amount: <b className="text-rose-600">INR {activeFineAction.amount.toFixed(2)}</b></p>
                  </div>

                  {activeFineAction.type === 'CASH' && (
                    <div>
                      <label className="font-semibold block mb-1">Receipt / Counter Notes</label>
                      <input
                        type="text"
                        required
                        value={fineCashNotes}
                        onChange={(e) => setFineCashNotes(e.target.value)}
                        className="w-full px-3 py-2 border rounded-control"
                      />
                    </div>
                  )}

                  {activeFineAction.type === 'ADJUST' && (
                    <>
                      <div>
                        <label className="font-semibold block mb-1">Adjusted Amount (INR)</label>
                        <input
                          type="number"
                          step="0.25"
                          min="0"
                          max={activeFineAction.amount}
                          required
                          value={fineAdjustAmount}
                          onChange={(e) => setFineAdjustAmount(parseFloat(e.target.value) || 0)}
                          className="w-full px-3 py-2 border rounded-control"
                        />
                      </div>
                      <div>
                        <label className="font-semibold block mb-1">Reason for Adjustment</label>
                        <input
                          type="text"
                          required
                          value={fineAdjustReason}
                          onChange={(e) => setFineAdjustReason(e.target.value)}
                          className="w-full px-3 py-2 border rounded-control"
                        />
                      </div>
                    </>
                  )}

                  {activeFineAction.type === 'WAIVE' && (
                    <div>
                      <label className="font-semibold block mb-1">Waiver Justification</label>
                      <textarea
                        required
                        rows={2}
                        value={fineWaiveReason}
                        onChange={(e) => setFineWaiveReason(e.target.value)}
                        placeholder="e.g. Medical dispensation or Dean approval"
                        className="w-full px-3 py-2 border rounded-control"
                      />
                    </div>
                  )}

                  <div className="flex justify-end gap-2 pt-2">
                    <button
                      type="button"
                      onClick={() => setActiveFineAction(null)}
                      className="px-3 py-1.5 rounded-control border text-slate-600 hover:bg-slate-50 font-bold cursor-pointer"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="px-4 py-1.5 rounded-control bg-vidya-blue hover:bg-vidya-navy text-white font-bold cursor-pointer"
                    >
                      Confirm Action
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* VIEW 7: METADATA CRAWLER                                      */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'crawler' && (
        <div className="space-y-6">
          <div className="bg-white rounded-panel p-6 border border-vidya-border shadow-subtle">
            <h2 className="text-base font-bold text-vidya-navy flex items-center gap-2 mb-2">
              <Globe className="w-5 h-5 text-cyan-600" />
              <span>External Book Metadata Crawler</span>
            </h2>
            <p className="text-xs text-vidya-text-muted mb-4">
              Simulates web crawling of academic repositories with MD5-fingerprint deduplication.
            </p>

            <div className="flex gap-2">
              <input
                type="text"
                value={sourceUrl}
                onChange={(e) => setSourceUrl(e.target.value)}
                className="flex-1 px-3 py-2 border rounded-control text-xs font-mono"
              />
              <button
                onClick={handleTriggerCrawler}
                disabled={isCrawling}
                className="flex items-center gap-2 px-4 py-2 bg-cyan-600 hover:bg-cyan-700 text-white text-xs font-bold rounded-control transition-colors cursor-pointer"
              >
                <Play className="w-4 h-4" />
                <span>{isCrawling ? 'Crawling...' : 'Trigger Crawler'}</span>
              </button>
            </div>
          </div>

          <div className="bg-white rounded-panel border border-vidya-border shadow-subtle overflow-hidden">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 font-bold border-b border-vidya-border">
                <tr>
                  <th className="py-2.5 px-3">Job ID</th>
                  <th className="py-2.5 px-3">Fetched</th>
                  <th className="py-2.5 px-3">Parsed</th>
                  <th className="py-2.5 px-3">Imported</th>
                  <th className="py-2.5 px-3">Deduped</th>
                  <th className="py-2.5 px-3">Throughput</th>
                  <th className="py-2.5 px-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-vidya-border text-slate-700">
                {crawlerJobs.map((j) => (
                  <tr key={j.id}>
                    <td className="py-2.5 px-3 font-mono font-bold text-vidya-blue">{j.jobId}</td>
                    <td className="py-2.5 px-3">{j.recordsFetched}</td>
                    <td className="py-2.5 px-3">{j.recordsParsed}</td>
                    <td className="py-2.5 px-3 font-bold text-emerald-600">+{j.recordsImported}</td>
                    <td className="py-2.5 px-3 text-amber-600">{j.recordsDeduplicated}</td>
                    <td className="py-2.5 px-3 font-mono">{j.throughputBooksPerSec.toFixed(1)} b/s</td>
                    <td className="py-2.5 px-3">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800">
                        {j.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* VIEW 8: AUDIT TRAIL                                           */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'audit' && (
        <div className="bg-white rounded-panel p-6 border border-vidya-border shadow-subtle space-y-4">
          <div className="flex items-center justify-between pb-4 border-b border-vidya-border">
            <div>
              <h2 className="text-base font-bold text-vidya-navy flex items-center gap-2">
                <FileText className="w-5 h-5 text-slate-700" />
                <span>Security & Operations Audit Trail</span>
              </h2>
              <p className="text-xs text-vidya-text-muted mt-0.5">Chronological record of staff actions for accountability</p>
            </div>
            <button
              onClick={fetchAuditLogs}
              className="px-3 py-1.5 rounded-control border text-xs font-semibold hover:bg-slate-50 flex items-center gap-1.5 cursor-pointer"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>Refresh Log</span>
            </button>
          </div>

          <div className="border border-vidya-border rounded-control overflow-hidden">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 font-bold border-b border-vidya-border">
                <tr>
                  <th className="py-2.5 px-3">Timestamp</th>
                  <th className="py-2.5 px-3">Staff Operator</th>
                  <th className="py-2.5 px-3">Action</th>
                  <th className="py-2.5 px-3">Resource</th>
                  <th className="py-2.5 px-3">Target ID</th>
                  <th className="py-2.5 px-3">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-vidya-border text-slate-700">
                {auditLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-50 transition-colors">
                    <td className="py-2.5 px-3 text-slate-500 font-mono text-[11px]">
                      {new Date(log.timestamp).toLocaleTimeString()} · {new Date(log.timestamp).toLocaleDateString()}
                    </td>
                    <td className="py-2.5 px-3 font-semibold">{log.staffEmail}</td>
                    <td className="py-2.5 px-3">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-800 font-mono">
                        {log.action}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 font-mono text-slate-500">{log.targetResource}</td>
                    <td className="py-2.5 px-3 font-mono font-bold text-vidya-blue">{log.targetId}</td>
                    <td className="py-2.5 px-3 text-slate-600">{log.details}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* VIEW 9: STAFF & ACCESS CONTROL (ADMIN ONLY)                   */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'staff' && isAdmin && (
        <div className="space-y-6">
          <div className="bg-white rounded-panel p-6 border-2 border-purple-200 shadow-subtle space-y-4">
            <div className="flex items-center gap-2 pb-3 border-b border-purple-100">
              <Shield className="w-5 h-5 text-purple-700" />
              <h2 className="text-base font-bold text-vidya-navy">Staff & Role Administration (Admin Only)</h2>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {/* Create Staff Form */}
              <div className="p-4 rounded-control bg-purple-50/50 border border-purple-200 space-y-3">
                <h3 className="font-bold text-xs text-purple-900 uppercase">Create Staff Account</h3>
                <form onSubmit={handleCreateStaff} className="space-y-2.5 text-xs">
                  <div>
                    <label className="font-semibold block mb-1">Full Name</label>
                    <input
                      type="text"
                      required
                      value={staffName}
                      onChange={(e) => setStaffName(e.target.value)}
                      placeholder="e.g. S. Ramanujan"
                      className="w-full px-2.5 py-1.5 border rounded-control bg-white"
                    />
                  </div>
                  <div>
                    <label className="font-semibold block mb-1">Email</label>
                    <input
                      type="email"
                      required
                      value={staffEmail}
                      onChange={(e) => setStaffEmail(e.target.value)}
                      placeholder="e.g. librarian2@ssn.edu.in"
                      className="w-full px-2.5 py-1.5 border rounded-control bg-white"
                    />
                  </div>
                  <div>
                    <label className="font-semibold block mb-1">Temporary Password</label>
                    <input
                      type="text"
                      required
                      value={staffPassword}
                      onChange={(e) => setStaffPassword(e.target.value)}
                      className="w-full px-2.5 py-1.5 border rounded-control bg-white font-mono"
                    />
                  </div>
                  <div>
                    <label className="font-semibold block mb-1">Role Assignment</label>
                    <select
                      value={staffRole}
                      onChange={(e) => setStaffRole(e.target.value as Role)}
                      className="w-full px-2.5 py-1.5 border rounded-control bg-white"
                    >
                      <option value="LIBRARIAN">LIBRARIAN (Desk & Inventory)</option>
                      <option value="ADMIN">ADMINISTRATOR (Full System)</option>
                    </select>
                  </div>
                  <button
                    type="submit"
                    className="w-full py-2 bg-purple-700 hover:bg-purple-800 text-white font-bold rounded-control cursor-pointer"
                  >
                    Provision Staff Account
                  </button>
                </form>
              </div>

              {/* Staff Table */}
              <div className="md:col-span-2 border border-vidya-border rounded-control overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 text-slate-600 font-bold border-b border-vidya-border">
                    <tr>
                      <th className="py-2.5 px-3">Staff Name</th>
                      <th className="py-2.5 px-3">Email</th>
                      <th className="py-2.5 px-3">Role</th>
                      <th className="py-2.5 px-3">Status</th>
                      <th className="py-2.5 px-3 text-right">Access Controls</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-vidya-border text-slate-700">
                    {staffAccounts.map((s) => {
                      const isSelf = s.email === currentUser?.email;
                      return (
                        <tr key={s.id} className="hover:bg-slate-50 transition-colors">
                          <td className="py-2.5 px-3 font-semibold">
                            {s.name}
                            {isSelf && (
                              <span className="ml-2 text-[10px] text-purple-700 font-bold bg-purple-50 border border-purple-200 px-1.5 py-0.5 rounded">
                                You
                              </span>
                            )}
                          </td>
                          <td className="py-2.5 px-3 text-slate-600">{s.email}</td>
                          <td className="py-2.5 px-3">
                            <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                              s.role === 'ADMIN' ? 'bg-purple-100 text-purple-900 border border-purple-200' : 'bg-blue-100 text-blue-900'
                            }`}>
                              {s.role}
                            </span>
                          </td>
                          <td className="py-2.5 px-3">
                            <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                              s.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'
                            }`}>
                              {s.status}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 text-right space-x-2">
                            <button
                              onClick={() => handleToggleStaffRole(s.id, s.role, s.email)}
                              disabled={isSelf}
                              title={isSelf ? 'Self-demotion disabled' : 'Toggle role'}
                              className={`px-2 py-1 rounded text-[11px] font-bold transition-all ${
                                isSelf
                                  ? 'bg-slate-50 text-slate-400 cursor-not-allowed border border-slate-200'
                                  : 'bg-slate-100 hover:bg-slate-200 text-slate-800 cursor-pointer'
                              }`}
                            >
                              {s.role === 'LIBRARIAN' ? 'Promote to Admin' : 'Demote to Librarian'}
                            </button>
                            <button
                              onClick={() => handleToggleStaffActive(s.id, s.email)}
                              disabled={isSelf}
                              title={isSelf ? 'Cannot deactivate logged-in account' : 'Toggle status'}
                              className={`px-2 py-1 rounded text-[11px] font-bold transition-all ${
                                isSelf
                                  ? 'bg-slate-50 text-slate-400 cursor-not-allowed border border-slate-200'
                                  : 'bg-slate-100 hover:bg-slate-200 text-slate-800 cursor-pointer'
                              }`}
                            >
                              {s.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* VIEW 10: SYSTEM CONFIGURATION (ADMIN ONLY)                    */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'config' && isAdmin && (
        <div className="bg-white rounded-panel p-6 border-2 border-purple-200 shadow-subtle space-y-4">
          <div className="flex items-center gap-2 pb-3 border-b border-purple-100">
            <Settings className="w-5 h-5 text-purple-700" />
            <h2 className="text-base font-bold text-vidya-navy">Library System Policies & Operational Rules</h2>
          </div>

          <form onSubmit={handleSaveConfig} className="grid grid-cols-1 md:grid-cols-2 gap-6 text-xs">
            <div className="space-y-4">
              <div>
                <label className="font-bold block text-slate-700 mb-1">
                  Default Loan Period (Days)
                </label>
                <input
                  type="number"
                  min="1"
                  max="90"
                  value={sysConfig.loanPeriodDays}
                  onChange={(e) => setSysConfig({ ...sysConfig, loanPeriodDays: parseInt(e.target.value) || 14 })}
                  className="w-full px-3 py-2 border rounded-control"
                />
                <p className="text-[11px] text-slate-500 mt-0.5">Duration granted when checkout is created without explicit override</p>
              </div>

              <div>
                <label className="font-bold block text-slate-700 mb-1">
                  Default Borrowing Cap (Books per Member)
                </label>
                <input
                  type="number"
                  min="1"
                  max="50"
                  value={sysConfig.defaultBorrowLimit}
                  onChange={(e) => setSysConfig({ ...sysConfig, defaultBorrowLimit: parseInt(e.target.value) || 5 })}
                  className="w-full px-3 py-2 border rounded-control"
                />
                <p className="text-[11px] text-slate-500 mt-0.5">Maximum concurrent active loans before desk checkout blocks</p>
              </div>

              <div>
                <label className="font-bold block text-slate-700 mb-1">
                  Daily Overdue Fine Rate (INR per Day)
                </label>
                <input
                  type="number"
                  step="0.25"
                  min="0"
                  value={sysConfig.dailyFineRate}
                  onChange={(e) => setSysConfig({ ...sysConfig, dailyFineRate: parseFloat(e.target.value) || 1.5 })}
                  className="w-full px-3 py-2 border rounded-control"
                />
                <p className="text-[11px] text-slate-500 mt-0.5">Automated fine computed per calendar day overdue upon return</p>
              </div>
            </div>

            <div className="space-y-4">
              <div>
                <label className="font-bold block text-slate-700 mb-1">
                  Hold Pickup Expiration (Hours)
                </label>
                <input
                  type="number"
                  min="12"
                  max="168"
                  value={sysConfig.holdExpiryHours}
                  onChange={(e) => setSysConfig({ ...sysConfig, holdExpiryHours: parseInt(e.target.value) || 48 })}
                  className="w-full px-3 py-2 border rounded-control"
                />
                <p className="text-[11px] text-slate-500 mt-0.5">Hours reserved books are held on pickup shelf before automatic release</p>
              </div>

              <div>
                <label className="font-bold block text-slate-700 mb-1">
                  Maximum Renewal Count
                </label>
                <input
                  type="number"
                  min="0"
                  max="10"
                  value={sysConfig.maxRenewalCount}
                  onChange={(e) => setSysConfig({ ...sysConfig, maxRenewalCount: parseInt(e.target.value) || 2 })}
                  className="w-full px-3 py-2 border rounded-control"
                />
                <p className="text-[11px] text-slate-500 mt-0.5">Maximum times a member can self-extend an active loan</p>
              </div>

              <div className="p-3 rounded-control bg-rose-50 border border-rose-200">
                <label className="flex items-center gap-2 cursor-pointer font-bold text-rose-900">
                  <input
                    type="checkbox"
                    checked={sysConfig.maintenanceMode}
                    onChange={(e) => setSysConfig({ ...sysConfig, maintenanceMode: e.target.checked })}
                    className="rounded text-rose-600"
                  />
                  <span>System Maintenance Lock</span>
                </label>
                <p className="text-[11px] text-rose-700 mt-1">Temporarily block borrowing transactions for database migration</p>
              </div>
            </div>

            <div className="md:col-span-2 pt-2">
              <button
                type="submit"
                className="px-6 py-2.5 rounded-control bg-purple-700 hover:bg-purple-800 text-white font-bold shadow-subtle transition-colors cursor-pointer"
              >
                Save System Policies
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};

