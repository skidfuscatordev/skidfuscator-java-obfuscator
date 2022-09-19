package org.mapleir.ir.cfg;

import org.mapleir.ir.cfg.builder.ssa.BlockBuilder;
import org.mapleir.ir.cfg.builder.ssa.CfgBuilder;
import org.mapleir.ir.cfg.builder.ssa.expr.*;
import org.mapleir.ir.cfg.builder.ssa.expr.invoke.StaticInvocationExprBuilder;
import org.mapleir.ir.cfg.builder.ssa.expr.invoke.VirtualInvocationExprBuilder;
import org.mapleir.ir.cfg.builder.ssa.stmt.*;
import org.mapleir.ir.cfg.builder.ssa.stmt.copy.CopyPhiStmtBuilder;
import org.mapleir.ir.cfg.builder.ssa.stmt.copy.CopyVarStmtBuilder;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;

public interface SSAFactory {
    CfgBuilder cfg();

    BlockBuilder block();

    AllocObjectExprBuilder alloc_object_expr();

    ArithmeticExprBuilder arithmetic_expr();

    ArrayLengthExprBuilder array_length_expr();

    ArrayLoadExprBuilder array_load_expr();

    CastExprBuilder cast_expr();

    CaughtExceptionExprBuilder caught_exception_expr();

    ComparisonExprBuilder comparison_expr();

    ConstantExprBuilder constant_expr();

    FieldLoadExprBuilder field_load_expr();

    InstanceofExprBuilder instance_of_expr();

    NegationExprBuilder negation_expr();

    NewArrayExprBuilder new_array_expr();

    PhiExceptionExprBuilder phi_exception_expr();

    PhiExprBuilder phi_expr();

    VarExprBuilder var_expr();

    StaticInvocationExprBuilder static_invoke_expr();

    VirtualInvocationExprBuilder virtual_invoke_expr();

    CopyPhiStmtBuilder copy_phi_stmt();

    CopyVarStmtBuilder copy_var_stmt();

    ArrayStoreStmtBuilder array_store_stmt();

    ConditionalJumpStmtBuilder conditional_jump_stmt();

    FieldStoreStmtBuilder field_store_stmt();

    MonitorStmtBuilder monitor_stmt();

    NopStmtBuilder nop_stmt();

    PopStmtBuilder pop_stmt();

    ReturnStmtBuilder return_stmt();

    SwitchStmtBuilder switch_stmt();

    ThrowStmtBuilder throw_stmt();

    UnconditionalJumpStmtBuilder unconditional_jump_stmt();

}
